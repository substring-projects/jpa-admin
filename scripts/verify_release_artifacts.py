"""Check local release artifacts without signing, uploading or reading credentials.

Run after: mvn -Prelease -Dgpg.skip=true clean install
"""
from pathlib import Path
import re
import struct
import argparse
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--allow-snapshot", action="store_true", help="Check classifier packaging on development CI builds")
args = parser.parse_args()
parent = ET.parse(ROOT / "pom.xml").getroot()
version = parent.findtext("m:version", namespaces=NS)
assert version, "Missing project version"
assert args.allow_snapshot or not version.endswith("-SNAPSHOT"), "Expected a non-snapshot release version"
modules = [item.text for item in parent.findall("m:modules/m:module", NS)]
for module in modules:
    pom = ET.parse(ROOT / module / "pom.xml").getroot()
    assert pom.findtext("m:parent/m:version", namespaces=NS) == version, module
    if module == "jpa-admin-sample-app":
        continue
    for suffix in ("", "-sources", "-javadoc"):
        artifact = ROOT / module / "target" / f"{module}-{version}{suffix}.jar"
        assert artifact.is_file(), f"Missing {artifact}"
        with zipfile.ZipFile(artifact) as archive:
            assert archive.testzip() is None, f"Corrupt archive: {artifact}"
            entries = archive.namelist()
            if suffix:
                assert any(not entry.startswith("META-INF/") and not entry.endswith("/") for entry in entries), f"Empty classifier: {artifact}"
            if not suffix:
                for entry in entries:
                    if entry.endswith(".class"):
                        major = struct.unpack(">H", archive.read(entry)[6:8])[0]
                        assert major <= 61, f"Not Java 17 compatible: {artifact}: {entry}"
    print(f"OK {module}:{version}: main, sources, documentation")

with zipfile.ZipFile(ROOT / "jpa-admin-ui/target" / f"jpa-admin-ui-{version}.jar") as archive:
    prefix = "META-INF/resources/admin/"
    html = archive.read(prefix + "index.html").decode()
    assets = re.findall(r'(?:src|href)="\./(assets/[^\"]+)"', html)
    assert assets, "No UI asset references in packaged index"
    for asset in assets:
        assert prefix + asset in archive.namelist(), f"Missing UI asset: {asset}"
with zipfile.ZipFile(ROOT / "jpa-admin-identity/target" / f"jpa-admin-identity-{version}.jar") as archive:
    for migration in ("jpa-admin/migrations/V1__admin_identity.sql", "jpa-admin/migrations/mysql/V1__admin_identity.sql"):
        assert migration in archive.namelist(), f"Missing migration: {migration}"
print("OK packaged UI assets, identity migrations and Java 17 bytecode")
print("Signatures, Central validation and public resolution remain separate checks.")
