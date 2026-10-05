#include <filesystem>
#include <fstream>
#include <iostream>
#include <regex>
#include <string>
#include <vector>
#include <algorithm>

namespace fs = std::filesystem;

struct Finding {
    std::string severity;
    std::string area;
    std::string message;
};

static std::string read_file(const fs::path& p) {
    std::ifstream in(p, std::ios::binary);
    if (!in) return {};
    return {std::istreambuf_iterator<char>(in), std::istreambuf_iterator<char>()};
}

static bool source_like(const fs::path& p) {
    const auto e = p.extension().string();
    return e == ".kt" || e == ".kts" || e == ".java" || e == ".xml" ||
           e == ".yml" || e == ".yaml" || e == ".gradle" || e == ".md" ||
           e == ".json" || e == ".toml" || e == ".properties" ||
           e == ".cpp" || e == ".h" || e == ".hpp";
}

static bool has_any(const std::string& s, const std::vector<std::string>& needles) {
    for (const auto& n : needles) {
        if (s.find(n) != std::string::npos) return true;
    }
    return false;
}

static void require_text(
    const std::string& all,
    const std::vector<std::string>& needles,
    const std::string& area,
    const std::string& message,
    std::vector<Finding>& findings
) {
    if (!has_any(all, needles)) {
        findings.push_back({"HIGH", area, message});
    }
}

int main(int argc, char** argv) {
    const fs::path root = argc > 1 ? fs::path(argv[1]) : fs::current_path();
    std::vector<Finding> findings;
    std::size_t files = 0;
    std::size_t source_files = 0;
    std::string all_source;

    if (!fs::exists(root)) {
        std::cerr << "CIADI AUDIT ERROR: path does not exist: " << root << "\n";
        return 2;
    }

    const fs::path android_root = root / "android-source";
    if (!fs::exists(android_root)) {
        findings.push_back({"HIGH", "ANDROID", "Diretório android-source não encontrado."});
    }

    const std::regex query_token(
        R"(([?&](token|access_token|jwt|authorization)=))",
        std::regex::icase
    );
    const std::regex cleartext(
        R"(android:usesCleartextTraffic\s*=\s*"true")",
        std::regex::icase
    );
    const std::regex service_role(
        R"(service_role\s*[:=])",
        std::regex::icase
    );
    const std::regex private_key(
        R"(-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----)"
    );

    for (const auto& entry : fs::recursive_directory_iterator(root)) {
        if (!entry.is_regular_file()) continue;

        const auto p = entry.path();
        const auto ps = p.string();

        if (ps.find("/.git/") != std::string::npos ||
            ps.find("\\.git\\") != std::string::npos ||
            ps.find("/build/") != std::string::npos ||
            ps.find("\\build\\") != std::string::npos) {
            continue;
        }

        ++files;
        if (!source_like(p)) continue;
        ++source_files;

        const auto content = read_file(p);
        if (content.empty()) continue;
        all_source += "\n" + content;

        if (std::regex_search(content, query_token)) {
            findings.push_back({
                "HIGH", "AUTH",
                ps + ": token/JWT em URL query."
            });
        }

        if (std::regex_search(content, cleartext)) {
            findings.push_back({
                "HIGH", "TRANSPORT",
                ps + ": cleartext traffic ativo."
            });
        }

        if (std::regex_search(content, service_role)) {
            findings.push_back({
                "CRITICAL", "SECRETS",
                ps + ": possível service_role atribuído."
            });
        }

        if (std::regex_search(content, private_key)) {
            findings.push_back({
                "CRITICAL", "SECRETS",
                ps + ": chave privada detectada."
            });
        }
    }

    if (fs::exists(android_root)) {
        if (!fs::exists(android_root / "src/main/AndroidManifest.xml")) {
            findings.push_back({
                "HIGH", "ANDROID",
                "AndroidManifest.xml não encontrado no módulo real."
            });
        }
        if (!fs::exists(android_root / "src/test")) {
            findings.push_back({"MEDIUM", "TESTS", "Testes unitários ausentes."});
        }
        if (!fs::exists(android_root / "src/androidTest")) {
            findings.push_back({"MEDIUM", "TESTS", "Testes instrumentados ausentes."});
        }
    }

    require_text(
        all_source,
        {"SUBMIT_CLINICAL_FORM"},
        "AT",
        "SUBMIT_CLINICAL_FORM não encontrado.",
        findings
    );
    require_text(
        all_source,
        {"ACOMPANHAMENTO_ABA_ABC", "ABC", "ABA"},
        "AT_FORMS",
        "Contrato ABC/ABA não encontrado.",
        findings
    );
    require_text(
        all_source,
        {"DIAGNOSTICO_AVALIACAO", "AVALIACAO_NEURODESENVOLVIMENTO"},
        "AT_FORMS",
        "Diagnóstico/avaliação não encontrado.",
        findings
    );
    require_text(
        all_source,
        {"ClinicalFormsRepository", "submeterFormularioClinico"},
        "CLINICAL_FORMS",
        "Fluxo de submissão clínica não encontrado.",
        findings
    );
    require_text(
        all_source,
        {"ciadi_formularios_clinicos", "documentos_clinicos"},
        "CLINICAL_DOCUMENTS",
        "Tabelas/repositórios clínicos não encontrados.",
        findings
    );
    require_text(
        all_source,
        {"ciadi_criar_alerta_sos", "enviarAlertaSos"},
        "SOS",
        "Fluxo SOS não encontrado.",
        findings
    );
    require_text(
        all_source,
        {"registrarDenunciaBullying", "Bullying"},
        "BULLYING",
        "Fluxo Bullying não encontrado.",
        findings
    );
    require_text(
        all_source,
        {"WhatsApp", "ACTION_DIAL", "mailto:"},
        "SOS_CONTACTS",
        "Contacto SOS não encontrado.",
        findings
    );
    require_text(
        all_source,
        {"CiadiDocumentA4View", "ClinicalFormPrintAdapter"},
        "DOCUMENTS",
        "Componentes A4/impressão não encontrados.",
        findings
    );

    const fs::path permission_file =
        android_root / "src/main/java/com/example/domain/model/Permission.kt";

    if (!fs::exists(permission_file)) {
        findings.push_back({
            "HIGH", "ROLES",
            "Permission.kt não encontrado."
        });
    } else {
        const auto permission_text = read_file(permission_file);
        const auto at_pos = permission_text.find("UserRole.AT");
        const auto professional_pos =
            at_pos == std::string::npos
                ? std::string::npos
                : permission_text.find("UserRole.PROFISSIONAL", at_pos);

        const auto at_block =
            at_pos == std::string::npos
                ? std::string{}
                : permission_text.substr(
                    at_pos,
                    professional_pos == std::string::npos
                        ? 6000
                        : professional_pos - at_pos
                );

        if (at_pos == std::string::npos ||
            at_block.find("SUBMIT_CLINICAL_FORM") == std::string::npos) {
            findings.push_back({
                "CRITICAL", "ROLES",
                "A.T. sem SUBMIT_CLINICAL_FORM na matriz de permissões."
            });
        }
    }

    std::cout << "CIADI UBUNTU/C++ AUDIT v2\n";
    std::cout << "root=" << root << "\n";
    std::cout << "files_scanned=" << files << "\n";
    std::cout << "source_like_files=" << source_files << "\n";
    std::cout << "findings=" << findings.size() << "\n";

    for (const auto& finding : findings) {
        std::cout << "["
                  << finding.severity
                  << "] "
                  << finding.area
                  << " - "
                  << finding.message
                  << "\n";
    }

    const bool blocking = std::any_of(
        findings.begin(),
        findings.end(),
        [](const Finding& finding) {
            return finding.severity == "HIGH" ||
                   finding.severity == "CRITICAL";
        }
    );

    std::cout << "status="
              << (blocking ? "REVIEW_REQUIRED" : "PASS_WITH_REVIEW")
              << "\n";

    return blocking ? 1 : 0;
}
