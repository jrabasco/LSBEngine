# LSBEngine
Lovely Scala Blogging Engine provides a basic blog CMS, all written with love, Scala and coffein! My own deployed version can be found [here](https://jrabasco.me).

# Usage

## Prerequisites

1. SBT (Scala Build Tool): download [here](http://www.scala-sbt.org/download.html).
2. MongoDB: installation instructions [here](https://docs.mongodb.com/manual/installation/).
3. Sass (Syntactically Awesome Style Sheets): installation instructions [here](http://sass-lang.com/install).
4. UglifyJs: installation instructions [here](https://github.com/mishoo/UglifyJS#install-npm).

## Environment

The server is configured with environment variables:

- `APP_CONTEXT`: context of the application (`PROD` or `DEV`, default is `DEV`)
- `SERVER_HOST`: the host on which to bind. Default used is `localhost` which makes it unreachable from the outside
- `PUBLIC_PORT`: the port for the public api (default `8080`)
- `ADMIN_PORT`: the port for the admin api (default `9090`)
- `REPOSITORY_LINK`: link given in the `server/info` route (default `https://github.com/jrabasco/LSBEngine`)
- `MONGO_HOST`: host for the mongo database (default `localhost`)
- `MONGO_CUST_PORT`: port of the mongo database (default `27017`)
- `MONGO_NAME`: name of the database (default `lsbengine`)
- `HASH_ITERATIONS`: number of iterations when hashing the password (default `300000`, should be high enough so that it 
takes around 1 second to perform a hash)
- `BLOG_OWNER_FIRST_NAME`: first name of the owner of the blog (default `Jeremy`)
- `BLOG_OWNER_LAST_NAME`: last name of the owner of the blog (default `Rabasco`)
- `BLOG_OWNER_PSEUDO`: owner's usual pseudonym (default empty)
- `BLOG_OWNER_GENDER`: owner's gender (default `male`, remember that it was built for me initially and I am a male, no discrimination here)
- `BLOG_SHORT_DESCRIPTION`: short description of the blog, for the RSS flux (default `My Personal Blog`)
- `BLOG_META_DESCRIPTION`: short description for the blog (default `My name is Jeremy Rabasco. I am a Computer Science major and I currently work at <JOB_HERE>.`)
- `CONTACT_ADDRESS`: email for the footer (default `rabasco.jeremy@gmail.com`)
- `GTAG_KEY`: Google analytics tracking key, leave empty if you do not wish to use Google analytics.
- `HEADER_TITLE`: title in the header (default `LSBEngine`)
- `IMAGES_LOCATION`: location where the uploaded images are saved (default `/home/jrabasco/images`)
- `SITE_URL`: the URL of the website (default `local.lsbengine.me`)
- `RESUME_REPO`: link to your resume repository, see the Resume section
- `DISCLAIMER`: A disclaimer that will appear in the footer (default `My opinions do not necessarily represent those of my employer.`)
- `COPYRIGHT`: copyright mention for the footer (defaults to nothing)
- `DEFAULT_POSTS_PER_PAGE`: number of posts per page when no parameter is supplied (default `10`)

## Adding/Removing users

To add a user run:

```
$ sbt
> run-main me.lsbengine.bin.UserManager add <username> <password>
```

To remove a user run:
```
$ sbt
> run-main me.lsbengine.bin.UserManager remove <username>
```

The usernames are case insensitive. Remember that all the users are considered _admin_ users.

## Resume

To make your resume available, you must provide a link to your resume repository. This repository must contain a Makefile so that when running `make` in the repository, it generates a file named `resume.pdf`. It will be then downloadable from the following route: `/assets/$firstname_$lastname_resume.pdf`. `$firstname` and `$lastname` are the provided names in lower case.

If no repository is provided, this just does not happen at all.
## Running

Clone the repository and run

```
$  make run
```

## Docker run

If you plan to run this on the production machine, make sure you set the `PUBLIC_DOMAINS` environment variable to be your actual list of valid public domains/subdomains, `ADMIN_DOMAINS` to be the location for your admin interface and `DOMAIN` to be just your domain name then run:

```
$ make build-docker
$ docker-compose up -d
```

Example of a valid domains setup script:
```bash
export PUBLIC_DOMAINS="public.lsbengine.me lsbengine.me blog.lsbengine.me www.lsbengine.me"
export ADMIN_DOMAINS="admin.lsbengine.me"
export DOMAIN="lsbengine.me"
```
