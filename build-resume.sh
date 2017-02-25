#!/bin/sh

set -e

if [[ ! -z "${RESUME_REPO}" ]]; then
	if [ ! -d "resume" ]; then
		git clone "$RESUME_REPO" resume
		pushd resume
		rm -f resume.pdf
		popd
	fi

	pushd resume

	UPSTREAM=${1:-'@{u}'}
	LOCAL=$(git rev-parse @)
	REMOTE=$(git rev-parse "$UPSTREAM")
	BASE=$(git merge-base @ "$UPSTREAM")

	if [ $LOCAL = $REMOTE ]; then
    echo "Up-to-date"
	elif [ $LOCAL = $BASE ]; then
		echo "Need to pull"
		git pull
		rm -f resume.pdf
	elif [ $REMOTE = $BASE ]; then
		echo "Need to push"
	else
		echo "Diverged"
	fi

	if [ ! -f resume.pdf ]; then
		make
	fi

	popd

	FIRSTNAME="${BLOG_OWNER_FIRST_NAME:-Jeremy}"
	LASTNAME="${BLOG_OWNER_LAST_NAME:-Rabasco}"

	firstname=$(echo "$FIRSTNAME" | tr '[:upper:]' '[:lower:]')
	lastname=$(echo "$LASTNAME" | tr '[:upper:]' '[:lower:]')

	cp resume/resume.pdf src/main/resources/assets/"$firstname"_"$lastname"_resume.pdf
fi
