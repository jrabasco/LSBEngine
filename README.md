# LSBEngine
Lovely Scala Blogging Engine provides a basic blog CMS, all written with love and Scala!

# Usage

## Prerequisites

1. SBT (Scala Build Tool): download [here](http://www.scala-sbt.org/download.html).
2. MongoDB: installation instructions [here](https://docs.mongodb.com/manual/installation/).
3. Sass (Syntactically Awesome Style Sheets): installation instructions [here](http://sass-lang.com/install).

## Environment

The server is configured with environment variables:

- `APP_CONTEXT`: context of the application (`PROD` or `DEV`, default is `DEV`)
- `SERVER_HOST`: the host on which to bind. Default used is `localhost` which makes it unreachable from the outside
- `PUBLIC_PORT`: the port for the public api (default `8080`)
- `ADMIN_PORT`: the port for the admin api (default `9090`)
- `REPOSITORY_LINK`: link given in the `server/info` route (default `https://github.com/jrabasco/SBlog`)
- `MONGO_HOST`: host for the mongo database (default `localhost`)
- `MONGO_PORT`: port of the mongo database (default `27017`)
- `MONGO_NAME`: name of the database (default `lsbengine`)
- `HASH_ITERATIONS`: number of iterations when hashing the password (default `300000`, should be high enough so that it 
takes around 1 second to perform a hash)
- `BLOG_OWNER`: name of the owner of the blog (default `Jérémy Rabasco`)
- `CONTACT_ADDRESS`: email for the footer (default `rabasco.jeremy@gmail.com`)

## Adding/Removing users

To add a user run:

```
$ sbt run-main me.lsbengine.bin.UserManager add <username> <password>
```

To remove a user run:
```
$ sbt run-main me.lsbengine.bin.UserManager remove <username>
```

The usernames are case insensitive. Remember that all the users are considered _admin_ users.

## Running

Clone the repository and run

```
$  sbt run
```
