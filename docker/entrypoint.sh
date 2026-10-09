
#!/bin/sh
# ===========================================================================
# FIXORA backend container entrypoint
#
# Reads Render Secret Files, builds a PKCS12 truststore, then runs Spring
# Boot as an unprivileged user. Certificates and passwords are not baked
# into the Docker image.
# ===========================================================================
set -eu

APP_USER="${FIXORA_APP_USER:-fixora}"
CERT_DIR="${FIXORA_CERT_DIR:-/app/certs}"
DEFAULT_TRUSTSTORE="file:${CERT_DIR}/aiven-truststore.p12"

# Resolve truststore path for the Linux container.
TRUSTSTORE_URL="${SPRING_DATASOURCE_SSL_TRUSTSTORE_URL:-}"
TRUSTSTORE_PATH="${TRUSTSTORE_URL#file:}"

case "${TRUSTSTORE_PATH}" in
    /app/*) ;;
    "") TRUSTSTORE_URL="${DEFAULT_TRUSTSTORE}" ;;
    [A-Za-z]:*|/*[A-Za-z]:*) TRUSTSTORE_URL="${DEFAULT_TRUSTSTORE}" ;;
    /*) ;;
    *) TRUSTSTORE_URL="${DEFAULT_TRUSTSTORE}" ;;
esac

TRUSTSTORE_PATH="${TRUSTSTORE_URL#file:}"
TRUSTSTORE_PASS="${SPRING_DATASOURCE_SSL_TRUSTSTORE_PASSWORD:-changeit}"

export SPRING_DATASOURCE_SSL_TRUSTSTORE_URL="${TRUSTSTORE_URL}"
export SPRING_DATASOURCE_SSL_TRUSTSTORE_TYPE="${SPRING_DATASOURCE_SSL_TRUSTSTORE_TYPE:-PKCS12}"
export SPRING_DATASOURCE_SSL_TRUSTSTORE_PASSWORD="${TRUSTSTORE_PASS}"

# Locate the CA certificate.
CA_FILE=""

if [ -n "${AIVEN_CA_FILE:-}" ]; then
    if [ -f "${AIVEN_CA_FILE}" ]; then
        CA_FILE="${AIVEN_CA_FILE}"
    else
        echo "fixora: ERROR AIVEN_CA_FILE does not exist." >&2
        exit 1
    fi
elif [ -n "${AIVEN_CA_PEM_BASE64:-}" ]; then
    mkdir -p "${CERT_DIR}"
    CA_FILE="${CERT_DIR}/aiven-ca.pem"
    if ! printf '%s' "${AIVEN_CA_PEM_BASE64}" | base64 -d > "${CA_FILE}"; then
        echo "fixora: ERROR could not decode AIVEN_CA_PEM_BASE64." >&2
        exit 1
    fi
fi

# Standard secret locations, including Render Secret Files.
if [ -z "${CA_FILE}" ]; then
    for cand in \
        /etc/secrets/aiven-ca.pem \
        /run/secrets/aiven-ca.pem \
        "${CERT_DIR}/aiven-ca.pem"
    do
        if [ -f "${cand}" ]; then
            CA_FILE="${cand}"
            break
        fi
    done
fi

# Search other PEM/CRT files in secret directories as a fallback.
if [ -z "${CA_FILE}" ]; then
    for dir in /etc/secrets /run/secrets; do
        if [ -d "${dir}" ]; then
            for f in "${dir}"/*.pem "${dir}"/*.crt; do
                if [ -f "${f}" ]; then
                    CA_FILE="${f}"
                    break
                fi
            done
        fi
        if [ -n "${CA_FILE}" ]; then
            break
        fi
    done
fi

# Build the truststore from the CA certificate.
if [ -n "${CA_FILE}" ]; then
    mkdir -p "$(dirname "${TRUSTSTORE_PATH}")"

    # Rebuild it each startup to avoid reusing a stale truststore.
    rm -f "${TRUSTSTORE_PATH}"

    keytool -importcert -noprompt \
        -alias aiven-mysql-ca \
        -file "${CA_FILE}" \
        -keystore "${TRUSTSTORE_PATH}" \
        -storetype PKCS12 \
        -storepass "${TRUSTSTORE_PASS}" >/dev/null

    chmod 400 "${TRUSTSTORE_PATH}"

    echo "fixora: built truststore at ${TRUSTSTORE_PATH} from ${CA_FILE}"

    # Ensure the application can read the generated truststore.
    if [ "$(id -u)" -eq 0 ]; then
        chown "${APP_USER}:${APP_USER}" "${TRUSTSTORE_PATH}"
    fi
else
    if printf '%s' "${SPRING_DATASOURCE_URL:-}" |
        grep -qi 'sslMode=VERIFY_CA'
    then
        echo "fixora: FATAL: SSL verification requires a CA certificate, but none was found." >&2
        exit 1
    else
        echo "fixora: no Aiven CA found; using the default trust store." >&2
    fi
fi

# Never launch the application as root.
if [ "$(id -u)" -eq 0 ]; then
    if ! id "${APP_USER}" >/dev/null 2>&1; then
        echo "fixora: FATAL: application user '${APP_USER}' does not exist." >&2
        exit 1
    fi

    if ! command -v setpriv >/dev/null 2>&1; then
        echo "fixora: FATAL: setpriv is required to drop root privileges." >&2
        exit 1
    fi

    APP_UID="$(id -u "${APP_USER}")"
    APP_GID="$(id -g "${APP_USER}")"
    export HOME="/home/${APP_USER}"

    exec setpriv \
        --reuid="${APP_UID}" \
        --regid="${APP_GID}" \
        --init-groups \
        -- java -Dserver.port="${PORT:-8080}" -jar /app/app.jar
fi

# If already running unprivileged, start Java directly.
if [ "$(id -u)" -eq 0 ]; then
    echo "fixora: FATAL: refusing to run Java as root." >&2
    exit 1
fi

exec java -Dserver.port="${PORT:-8080}" -jar /app/app.jar
