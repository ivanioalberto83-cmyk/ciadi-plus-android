#include <filesystem>
#include <fstream>
#include <iostream>
#include <regex>
#include <string>
#include <vector>
#include <algorithm>
namespace fs = std::filesystem;
struct Finding { std::string severity, area, message; };
static std::string read_file(const fs::path& p){ std::ifstream in(p,std::ios::binary); if(!in)return{}; return {std::istreambuf_iterator<char>(in),std::istreambuf_iterator<char>()}; }
static bool source_like(const fs::path& p){ auto e=p.extension().string(); return e==".kt"||e==".kts"||e==".java"||e==".xml"||e==".yml"||e==".yaml"||e==".gradle"||e==".md"||e==".json"||e==".toml"||e==".properties"||e==".cpp"||e==".h"||e==".hpp"; }
static bool has(const std::string&s,const std::vector<std::string>& ns){for(const auto&n:ns)if(s.find(n)!=std::string::npos)return true;return false;}
static void req(const std::string&a,const std::vector<std::string>&n,const std::string&area,const std::string&msg,std::vector<Finding>&f){if(!has(a,n))f.push_back({"HIGH",area,msg});}
int main(int argc,char**argv){
 fs::path root=argc>1?fs::path(argv[1]):fs::current_path(); std::vector<Finding> f; size_t files=0,src=0; std::string all;
 if(!fs::exists(root)){std::cerr<<"CIADI AUDIT ERROR: path does not exist: "<<root<<"\n";return 2;}
 fs::path ar=root/"android-source"; if(!fs::exists(ar))f.push_back({"HIGH","ANDROID","Diretório android-source não encontrado."});
 for(const auto&e:fs::recursive_directory_iterator(root)){
  if(!e.is_regular_file()) continue;\n  auto p=e.path();\n  auto ps=p.string();
  if(ps.find("/.git/")!=std::string::npos||ps.find("\\.git\\")!=std::string::npos||ps.find("/build/")!=std::string::npos||ps.find("\\build\\")!=std::string::npos)continue;
  ++files; if(!source_like(p))continue; ++src; auto c=read_file(p); if(c.empty())continue; all+="\n"+c;
  if(std::regex_search(c,std::regex(R"(([?&](token|access_token|jwt|authorization)=))",std::regex::icase)))f.push_back({"HIGH","AUTH",ps+": token/JWT em URL query."});
  if(std::regex_search(c,std::regex(R"(android:usesCleartextTraffic\s*=\s*"true")",std::regex::icase)))f.push_back({"HIGH","TRANSPORT",ps+": cleartext traffic ativo."});
  if(std::regex_search(c,std::regex(R"(service_role\s*[:=])",std::regex::icase)))f.push_back({"CRITICAL","SECRETS",ps+": possível service_role atribuído."});
  if(std::regex_search(c,std::regex(R"(-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----)")))f.push_back({"CRITICAL","SECRETS",ps+": chave privada detectada."});
 }
 if(!ar.empty()){
  if(!fs::exists(ar/"src/main/AndroidManifest.xml"))f.push_back({"HIGH","ANDROID","AndroidManifest.xml não encontrado no módulo real."});
  if(!fs::exists(ar/"src/test"))f.push_back({"MEDIUM","TESTS","Testes unitários ausentes."});
  if(!fs::exists(ar/"src/androidTest"))f.push_back({"MEDIUM","TESTS","Testes instrumentados ausentes."});
 }
 req(all,{"SUBMIT_CLINICAL_FORM"},"AT","SUBMIT_CLINICAL_FORM não encontrado.",f);
 req(all,{"ACOMPANHAMENTO_ABA_ABC","ABC","ABA"},"AT_FORMS","Contrato ABC/ABA não encontrado.",f);
 req(all,{"DIAGNOSTICO_AVALIACAO","AVALIACAO_NEURODESENVOLVIMENTO"},"AT_FORMS","Diagnóstico/avaliação não encontrado.",f);
 req(all,{"ClinicalFormsRepository","submeterFormularioClinico"},"CLINICAL_FORMS","Fluxo de submissão clínica não encontrado.",f);
 req(all,{"ciadi_formularios_clinicos","documentos_clinicos"},"CLINICAL_DOCUMENTS","Tabelas/repositórios clínicos não encontrados.",f);
 req(all,{"ciadi_criar_alerta_sos","enviarAlertaSos"},"SOS","Fluxo SOS não encontrado.",f);
 req(all,{"registrarDenunciaBullying","Bullying"},"BULLYING","Fluxo Bullying não encontrado.",f);
 req(all,{"WhatsApp","ACTION_DIAL","mailto:"},"SOS_CONTACTS","Contacto SOS não encontrado.",f);
 req(all,{"CiadiDocumentA4View","ClinicalFormPrintAdapter"},"DOCUMENTS","Componentes A4/impressão não encontrados.",f);
 auto pos=all.find("UserRole.AT"); if(pos==std::string::npos)pos=all.find("UserRole::AT");
 if(pos==std::string::npos)f.push_back({"HIGH","ROLES","UserRole AT não encontrado."}); else {auto end=all.find("UserRole.PROFISSIONAL",pos);if(end==std::string::npos)end=all.find("UserRole::PROFISSIONAL",pos);auto b=all.substr(pos,end==std::string::npos?6000:end-pos);if(b.find("SUBMIT_CLINICAL_FORM")==std::string::npos)f.push_back({"CRITICAL","ROLES","A.T. sem SUBMIT_CLINICAL_FORM."});}
 std::cout<<"CIADI UBUNTU/C++ AUDIT v2\nroot="<<root<<"\nfiles_scanned="<<files<<"\nsource_like_files="<<src<<"\nfindings="<<f.size()<<"\n";
 for(const auto&x:f)std::cout<<"["<<x.severity<<"] "<<x.area<<" - "<<x.message<<"\n";
 bool block=std::any_of(f.begin(),f.end(),[](const Finding&x){return x.severity=="HIGH"||x.severity=="CRITICAL";});
 std::cout<<"status="<<(block?"REVIEW_REQUIRED":"PASS_WITH_REVIEW")<<"\n"; return block?1:0;
}