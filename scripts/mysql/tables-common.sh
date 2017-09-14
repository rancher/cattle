set -e

REF=()

start()
{
    local name=$1
cat << EOF

DROP TABLE IF EXISTS \`$name\`;
CREATE TABLE \`$name\` (
  \`id\` bigint(19) NOT NULL AUTO_INCREMENT,
  \`name\` varchar(255) DEFAULT NULL,
EOF
    if [ $name != account ] && [ "$2" != "no_account" ];then
        ref account
    fi
cat << EOF
  \`kind\` varchar(255) NOT NULL,
  \`uuid\` varchar(128) NOT NULL,
  \`description\` varchar(1024) DEFAULT NULL,
  \`state\` varchar(128) NOT NULL,
  \`created\` datetime DEFAULT NULL,
  \`removed\` datetime DEFAULT NULL,
  \`remove_time\` datetime DEFAULT NULL,
  \`data\` text,
EOF
}

end()
{
    common_indexes $1
    end_table
}

index()
{
cat << EOF
  ${3}KEY \`idx_$1_$2\` (\`$2\`),
EOF
}

ref()
{
    REF+=($1)
    SUFFIX=" DEFAULT NULL"
    if [ "$2" = "notnull" ]; then
        SUFFIX=""
    fi
cat << EOF
  \`${1}_id\` bigint(19)$SUFFIX,
EOF
}

string()
{
    echo "  \`$1\` varchar(${2:-255}) DEFAULT NULL,"
}

bigint()
{
    echo "  \`$1\` bigint(19) DEFAULT NULL,"
}

int()
{
    echo "  \`$1\` int(10) DEFAULT NULL,"
}

bool()
{
    echo "  \`$1\` bit(1) NOT NULL DEFAULT b'${2:-0}',"
}

default_table()
{
    start $1
    end $1
}

map()
{
    start ${1}_${2}_map no_account
    ref $1 notnull
    ref $2 notnull
    end ${1}_${2}_map
}

common_indexes()
{
    local name=$1
    index $name name
    index $name removed
    index $name uuid "UNIQUE "
    index $name remove_time
    index $name state

    for ref in "${REF[@]}"; do
cat << EOF
  KEY \`fk_${name}_${ref}_id\` (\`${ref}_id\`),
  CONSTRAINT \`fk_${name}__${ref}_id\` FOREIGN KEY (\`${ref}_id\`) REFERENCES \`${ref}\` (\`id\`) ON DELETE RESTRICT ON UPDATE RESTRICT,
EOF
    done

    REF=()

cat << EOF
  PRIMARY KEY (\`id\`)
EOF
}

end_table()
{
    echo ') ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;'
}
