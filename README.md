Site generator for http://www.tomdalling.com/

Commonly Used Commands
----------------------

    lein midje
    lein midje :autotest
    lein ring server
    lein build-site
    ./publish.sh

TODO
----

 - Update OpenGL article 02 to include section about glBlendFunc
 - Don't load disqus in development. It's remembering incorrect post titles
   before they are published
 - Proper titles for category RSS feeds
 - Absolute URLs for RSS feeds (and maybe everywhere)
 - Test that follows all internal links to make sure they're correct
 - Turn on markdown options for automatic url linking, and common text
   transformations like "--" and "..."

Proofreading
------------

TODO: `set spell` for all markdown files in vimrc

Common typos:

 - therefor
 - its
 - it's
 - homogenous
 - colour (use American spelling)

Also check for:

 - TODO
 - Places to add pull quotes
 - markdown/quotes inside tags (like <blockquote>s and <figure>s)
 - Broken links
