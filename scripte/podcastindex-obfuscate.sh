#!/bin/sh

set -eu

usage() {
    cat <<'EOF'
Verwendung:
  ./scripte/podcastindex-obfuscate.sh PODCASTINDEX_API_KEY "klartext"
  ./scripte/podcastindex-obfuscate.sh PODCASTINDEX_API_SECRET "klartext"

Gibt einen Wert im Format obf:... aus, der als JVM-Property verwendet werden kann.
EOF
}

if [ "$#" -ne 2 ]; then
    usage >&2
    exit 1
fi

property_name=$1
plain_value=$2

case "$property_name" in
    PODCASTINDEX_API_KEY|PODCASTINDEX_API_SECRET|mediathek.audiothek.podcastindex.apiKey|mediathek.audiothek.podcastindex.apiSecret)
        ;;
    *)
        echo "Nicht unterstützte Property: $property_name" >&2
        usage >&2
        exit 1
        ;;
esac

perl -MDigest::SHA=sha256 -MMIME::Base64=encode_base64url -e '
    use strict;
    use warnings;
    use utf8;
    binmode STDOUT, ":utf8";

    my ($property_name, $plain_value) = @ARGV;
    my $salt = "MediathekView.PodcastIndex.v1:";
    my $key = sha256($salt . $property_name);
    my $result = "";

    for my $index (0 .. length($plain_value) - 1) {
        my $plain_byte = ord(substr($plain_value, $index, 1));
        my $key_byte = ord(substr($key, $index % length($key), 1));
        $result .= chr($plain_byte ^ $key_byte);
    }

    print "obf:", encode_base64url($result), "\n";
' "$property_name" "$plain_value"
