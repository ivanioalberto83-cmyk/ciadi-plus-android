#!/usr/bin/env python3
"""Apply the CIADI+ SOS contact-picker overlay to the extracted Android source.

Privacy design: use Android's system contact picker (ACTION_PICK via PickContact),
not READ_CONTACTS or a full address-book scan. Only the contact the user selects
is read through the temporary URI grant. Delivery is user-confirmed through
WhatsApp or the SMS composer; the app does not silently send messages.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1]).resolve()
screen = root / "src/main/java/com/example/ui/screens/sos/SosScreen.kt"
if not screen.is_file():
    raise SystemExit(f"SosScreen.kt not found: {screen}")
text = screen.read_text(encoding="utf-8")
if 'sos_choose_contact' in text and 'ActivityResultContracts.PickContact()' in text:
    print("SOS contact picker is already integrated in source; skipping overlay.")
    raise SystemExit(0)

def replace_once(old: str, new: str, label: str):
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one anchor for {label}; found {count}. Refusing unsafe patch.")
    text = text.replace(old, new, 1)

replace_once(
    "import android.content.Context\n",
    "import android.content.Context\nimport android.content.Intent\nimport android.net.Uri\nimport android.provider.ContactsContract\n",
    "Android contact and sharing imports"
)
replace_once(
    "    var mensagemErro by remember { mutableStateOf<String?>(null) }\n",
    """    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var contactoSOSNome by remember { mutableStateOf<String?>(null) }
    var contactoSOSNumero by remember { mutableStateOf<String?>(null) }
    var contactoSOSAviso by remember { mutableStateOf<String?>(null) }

    // O seletor do sistema só dá acesso ao contacto escolhido; não lemos a agenda inteira.
    val escolherContactoSOS = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri == null) {
            contactoSOSAviso = "Nenhum contacto selecionado."
        } else {
            try {
                var nome: String? = null
                var numero: String? = null
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                        nome = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                        context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )?.use { phones ->
                            if (phones.moveToFirst()) {
                                numero = phones.getString(phones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            }
                        }
                    }
                }
                if (numero.isNullOrBlank()) {
                    contactoSOSAviso = "O contacto escolhido não tem número de telefone disponível."
                    contactoSOSNome = null
                    contactoSOSNumero = null
                } else {
                    contactoSOSNome = nome ?: "Contacto selecionado"
                    contactoSOSNumero = numero
                    contactoSOSAviso = "Contacto selecionado. Escolha WhatsApp ou SMS para confirmar o envio."
                }
            } catch (_: Exception) {
                contactoSOSAviso = "Não foi possível ler o contacto escolhido. Tente novamente."
                contactoSOSNome = null
                contactoSOSNumero = null
            }
        }
    }
""",
    "contact selection state and picker"
)

replace_once(
    "            // 4. Localização Opcional com Consentimento Claro\n",
    """            // Contacto pessoal opcional: o Android apresenta o seletor do sistema.
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CIADIColors.SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Contacto de emergência pessoal (opcional)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CIADIColors.Brown
                            )
                        )
                        Text(
                            text = "O CIADI+ não consulta toda a sua agenda. O Android permite escolher apenas um contacto. O envio só acontece quando confirmar WhatsApp ou SMS.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                        )
                        OutlinedButton(
                            onClick = { escolherContactoSOS.launch(null) },
                            modifier = Modifier.fillMaxWidth().testTag("sos_choose_contact")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (contactoSOSNome == null) "Escolher contacto do telemóvel" else "Alterar contacto")
                        }
                        if (contactoSOSNome != null && contactoSOSNumero != null) {
                            Text(
                                text = "$contactoSOSNome · $contactoSOSNumero",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = CIADIColors.TextPrimary
                                )
                            )
                            val nomeAssistido = selectedPatient?.nome ?: user.activePatientName ?: "Assistido"
                            val mensagemPartilha = buildString {
                                append("ALERTA SOS CIADI+\\n")
                                append("Assistido: $nomeAssistido\\n")
                                append("Tipo: $tipoAlerta\\n")
                                append("Mensagem: $mensagemEmergencia\\n")
                                if (autorizarLocalizacao) {
                                    val loc = obterUltimaLocalizacao(context)
                                    if (loc != null) append("Localização: https://maps.google.com/?q=${loc.latitude},${loc.longitude}\\n")
                                }
                                append("Hora: ${java.time.ZonedDateTime.now().toString()}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        try {
                                            val digits = contactoSOSNumero.orEmpty().filter { it.isDigit() || it == '+' }.replace("+", "")
                                            val uri = Uri.parse("https://wa.me/$digits?text=" + Uri.encode(mensagemPartilha))
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            contactoSOSAviso = "WhatsApp aberto. Confirme o destinatário e toque em enviar."
                                        } catch (_: Exception) {
                                            contactoSOSAviso = "Não foi possível abrir WhatsApp. Experimente SMS."
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("sos_share_whatsapp")
                                ) { Text("WhatsApp") }
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("smsto:" + Uri.encode(contactoSOSNumero.orEmpty()))
                                                putExtra("sms_body", mensagemPartilha)
                                            }
                                            context.startActivity(intent)
                                            contactoSOSAviso = "Aplicação de SMS aberta. Confirme o destinatário e toque em enviar."
                                        } catch (_: Exception) {
                                            contactoSOSAviso = "Não foi possível abrir a aplicação de SMS."
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("sos_share_sms")
                                ) { Text("SMS") }
                            }
                        }
                        if (!contactoSOSAviso.isNullOrBlank()) {
                            Text(
                                text = contactoSOSAviso.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(color = CIADIColors.TextSecondary)
                            )
                        }
                    }
                }
            }

            // 4. Localização Opcional com Consentimento Claro
""",
    "SOS contact card"
)

screen.write_text(text, encoding="utf-8")
print("Applied privacy-preserving SOS contact picker overlay.")
