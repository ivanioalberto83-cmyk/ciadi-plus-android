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
    return std::string((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
}

int main(int argc, char** argv) {
    fs::path root = argc > 1 ? fs::path(argv[1]) : fs::current_path();
    std::vector<Finding> findings;
    std::size_t files = 0;
    std::size_t source_files = 0;

    if (!fs::exists(root)) {
        std::cerr << "CIADI AUDIT ERROR: path does not exist: " << root << "\n";
        return 2;
    }

    const std::regex secret_like(R"((service_role|anon[_-]?key|api[_-]?key|access[_-]?token|private[_-]?key))",
                                 std::regex::icase);
    const std::regex query_token(R"(([?&](token|access_token|jwt|authorization)=))",
                                 std::regex::icase);
    const std::regex dangerous_url(R"(http://)", std::regex::icase);

    for (const auto& entry : fs::recursive_directory_iterator(root)) {
        if (!entry.is_regular_file()) continue;
        const auto p = entry.path();
        const auto name = p.filename().string();
        if (name == ".git" || p.string().find("/build/") != std::string::npos ||
            p.string().find("\\build\\") != std::string::npos) continue;

        ++files;
        const auto ext = p.extension().string();
        if (ext == ".kt" || ext == ".kts" || ext == ".java" || ext == ".xml" ||
            ext == ".yml" || ext == ".yaml" || ext == ".gradle" || ext == ".md") {
            ++source_files;
        }

        const auto content = read_file(p);
        if (content.empty()) continue;

        if (std::regex_search(content, secret_like)) {
            findings.push_back({"WARNING", "SECRETS",
                p.string() + ": possible credential/key identifier found; review manually."});
        }
        if (std::regex_search(content, query_token)) {
            findings.push_back({"HIGH", "AUTH",
                p.string() + ": token-like value appears in a URL query string; avoid insecure token transport."});
        }
        if (std::regex_search(content, dangerous_url)) {
            findings.push_back({"WARNING", "TRANSPORT",
                p.string() + ": HTTP URL detected; confirm it is not used for clinical/auth traffic."});
        }
    }

    const fs::path manifest = root / "app/src/main/AndroidManifest.xml";
    if (!fs::exists(manifest)) {
        findings.push_back({"WARNING", "ANDROID", "AndroidManifest.xml not found at standard app source path."});
    } else {
        const auto m = read_file(manifest);
        if (m.find(R"(android:usesCleartextTraffic="true")") != std::string::npos) {
            findings.push_back({"HIGH", "TRANSPORT", "AndroidManifest enables cleartext traffic."});
        }
    }

    const fs::path tests = root / "app/src/test";
    if (!fs::exists(tests)) {
        findings.push_back({"WARNING", "TESTS", "Unit-test directory not found at standard app source path."});
    }

    std::cout << "CIADI UBUNTU/C++ AUDIT\n";
    std::cout << "root=" << root << "\n";
    std::cout << "files_scanned=" << files << "\n";
    std::cout << "source_like_files=" << source_files << "\n";
    std::cout << "findings=" << findings.size() << "\n";

    for (const auto& f : findings) {
        std::cout << "[" << f.severity << "] " << f.area << " - " << f.message << "\n";
    }

    const bool blocking = std::any_of(findings.begin(), findings.end(),
        [](const Finding& f){ return f.severity == "HIGH" || f.severity == "CRITICAL"; });

    std::cout << "status=" << (blocking ? "REVIEW_REQUIRED" : "PASS_WITH_REVIEW") << "\n";
    return blocking ? 1 : 0;
}
