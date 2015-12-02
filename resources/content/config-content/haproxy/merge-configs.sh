#!/bin/sh
set -e

merge_custom() {
    started=false
    while IFS='' read -r custom || [[ -n "$custom" ]]; do
        if [[ $custom == $1 ]];then
            started=true
        elif $started; then
            if [[ $custom == " "* ]]; then
                echo "$custom" >> $3
            else
                break
            fi
        fi
    done < $2
}

do_merge() {
    declare header
    merged=false
    while IFS='' read -r line || [[ -n "$line" ]]; do
        if ( ([[ $provider == "frontend"* ]] &&[[ $line == *"acl "* ]]) || ([[ $provider == "backend"* ]] && [[ $line == *"server "* ]]) || [[ $line != " "* ]]) && [ "$merged" = false ] ; then
            merge_custom "$header" $2 $3
            merged=true
        fi
        if [[ $line != " "* ]];then
            echo "" >> $3
            header=$line
            merged=false
        fi
        echo "$line" >> $3
    done < $1
}

append_custom() {
    started=false
    while IFS='' read -r line || [[ -n "$line" ]]; do
        if [[ $line != " "* ]];then
            if grep -q "$line" $1; then
                started=false
            else
                started=true
                echo "" >> $3
                echo "$line" >> $3
            fi
        elif $started; then
            echo "$line" >> $3
        fi
    done < $2
}


merge_configs() {
    defaultcfg="default.cfg"
    customcfg="custom.cfg"
    customcfgtemp="custom-temp.cfg"
    # remove ctrlM from the end of each line in custom config
    tr -d "\015" <$2 > $customcfgtemp
    # replace tabs with spaces
    # and trim empty or spaces only lines
    sed -e 's/\t/    /g' -e '/^ *$/d' -e '/^$/d' $1 > $defaultcfg
    sed -e 's/\t/    /g' -e '/^ *$/d' -e '/^$/d' $customcfgtemp > $customcfg
    rm -f $3
    # merge sections common for default and custom configs
    do_merge $defaultcfg $customcfg $3
    # append sections existing in custom and missing in default config
    append_custom $defaultcfg $customcfg $3
    # remove temporary files
    rm -rf $defaultcfg && rm -rf $customcfg
}

df