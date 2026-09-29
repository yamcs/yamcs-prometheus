#!/usr/bin/env bash
#
# Publishes the current checkout to the self-hosted Maven repository
# (a Google Cloud Storage bucket served at https://maven.yamcs.org).
#
# Bucket layout:
#   releases/<group path>/<artifactId>/...
#   snapshots/<group path>/<artifactId>/...
#
# Requires gcloud, logged in (`gcloud auth login`) with an account that can
# write to the bucket.
#
# Usage: ./publish.sh
#
# Environment:
#   MAVEN_BUCKET   bucket URL (default: gs://yamcs-maven)

set -euo pipefail

MAVEN_BUCKET="${MAVEN_BUCKET:-gs://yamcs-maven}"

evaluate() {
    mvn -q help:evaluate -Dexpression="$1" -DforceStdout
}

# Maven does not set <latest> in maven-metadata.xml (neither when creating it,
# nor when merging into it), so set it to the version just published, and
# update the checksums.
set_latest() {
    local file=$1 version=$2
    VERSION=$version perl -0pi -e '
        s#<latest>[^<]*</latest>#<latest>$ENV{VERSION}</latest># or
        s#(\s*)<versions>#$1<latest>$ENV{VERSION}</latest>$1<versions>#' "$file"
    for algorithm in md5 sha1 sha256 sha512; do
        printf '%s' "$(openssl dgst -$algorithm -r "$file" | cut -d' ' -f1)" > "$file.$algorithm"
    done
}

group_id=$(evaluate project.groupId)
artifact_id=$(evaluate project.artifactId)
version=$(evaluate project.version)

if [[ "$version" == *-SNAPSHOT ]]; then
    repo=snapshots
    profile=
else
    repo=releases
    profile=-Prelease
fi

artifact_path="${group_id//.//}/$artifact_id"
version_path="$artifact_path/$version"
remote="$MAVEN_BUCKET/$repo"
staging=$(mktemp -d)
trap 'rm -rf "$staging"' EXIT

echo "Publishing $group_id:$artifact_id:$version to $remote"

if [[ "$repo" == releases ]] && gcloud storage ls "$remote/$version_path/" &>/dev/null; then
    echo "Error: $version is already published" >&2
    exit 1
fi

# Seed the staging repository with the currently published metadata, so that
# Maven merges into it rather than producing metadata with only this version.
# (For snapshots, the version-level metadata holds the build number.)
seed() {
    mkdir -p "$staging/$1"
    if gcloud storage ls "$remote/$1/maven-metadata.xml" &>/dev/null; then
        gcloud storage cp "$remote/$1/maven-metadata.xml*" "$staging/$1/"
    fi
}
seed "$artifact_path"
if [[ "$repo" == snapshots ]]; then
    seed "$version_path"
fi

mvn clean deploy ${profile:+"$profile"} \
    -DaltDeploymentRepository="staging::file://$staging" \
    -Daether.checksums.algorithms=SHA-512,SHA-256,SHA-1,MD5

set_latest "$staging/$artifact_path/maven-metadata.xml" "$version"

# Artifacts first. These never change once published.
find "$staging/$version_path" -type f ! -name 'maven-metadata.xml*' \
    | gcloud storage cp -I "$remote/$version_path/" \
        --no-clobber \
        --cache-control='public, max-age=31536000, immutable'

# Metadata last, so it never references artifacts that are not uploaded yet.
upload_metadata() {
    gcloud storage cp "$staging/$1"/maven-metadata.xml* "$remote/$1/" \
        --cache-control='public, max-age=60'
}
upload_metadata "$artifact_path"
if [[ "$repo" == snapshots ]]; then
    upload_metadata "$version_path"
fi

echo "Published https://maven.yamcs.org/$repo/$version_path/"
