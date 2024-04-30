#!/bin/bash

TMP1=`mktemp`
TMP2=`mktemp`

function elimNumbers {
    sed -E "s/([0-9]+):/XX/" | sed -E "s/#([0-9]+)/XX/"
}

ORIG="out/$1.orig.class"
TRANS="out/$1.class"

echo "${ORIG}"
if [ ! -f "${ORIG}" ]; then
    echo Original class ${ORIG} missing.
    exit
fi
if [ ! -f "${TRANS}" ]; then
    echo Transformed class ${TRANS} missing.
    exit
fi

javap -private -v -c ${ORIG}  | elimNumbers > ${TMP1}
javap -private -v -c ${TRANS} | elimNumbers > ${TMP2}

# emacs --eval "(ediff-files \"${TMP1}\" \"${TMP2}\")"

# Note: zR shows all folds.
vimdiff ${TMP1} ${TMP2}

# diff -y ${TMP1} ${TMP2} | colordiff | less

rm -f ${TMP1} ${TMP2}
