#!/usr/bin/env bash

# Assuming that the binary is located at /usr/local/bin/bibcal.
FULL_OUTPUT=$(/usr/local/bin/bibcal -tv)
IFS=$'\n' read -r -d '' -a KEYS < <( awk -F'  ' '{ print $1 }' <<< "$FULL_OUTPUT" && printf '\0' )
IFS=$'\n' read -r -d '' -a VALUES < <( cut -c 25- <<< "$FULL_OUTPUT" && printf '\0' )
DATE=${VALUES[0]}
SABBATH=${VALUES[5]}
MAJOR_F=${VALUES[6]}

if [ ! "$MAJOR_F" = "false" ]; then
    MSG="$MAJOR_F, $DATE"
else
    MSG="$DATE"
fi

if [ "$SABBATH" = "true" ]; then
    echo "\033[0;33m$MSG\033[0m"
else
    echo "$MSG"
fi

echo "---"

for ((i=0;i<${#KEYS[@]};i++))
do
    printf '%-23s%s | font=Monaco\n' "${KEYS[$i]}" "${VALUES[$i]}"
done
