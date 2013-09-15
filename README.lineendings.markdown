# Fixed line endings

See https://www.kernel.org/pub/software/scm/git/docs/gitattributes.html
for details.

Here is what I did:

    $ echo "* text=auto" >.gitattributes
    $ rm .git/index
    $ git reset
    $ git status
    $ git status -sb\
      |cut -c4-\
      |grep -v HEAD\
      |xargs sed -i "s/\r//"
    $ git add -u
    $ git add .gitattributes
    $ git commit -m "Fixed line endings"

Note: It is pretty hard to merge this change, since all out-standing pull
requests will fail afterwards. If you applied any changes to your git branch
before applying this change, you'll get conflicts, too.

This URL shows the change on GitHub:
<https://github.com/uli-heller/moxie/commit/39e8b937fd41a4d97a5bc0150bb6dd88ae1ce0e6>.
Every line in every text file appears to have changed.

This URL shows only those changes which aren't related to the fixing of
the line endings:
<https://github.com/uli-heller/moxie/commit/39e8b937fd41a4d97a5bc0150bb6dd88ae1ce0e6?w=1>.
