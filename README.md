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

 - Don't load disqus in development. It's remembering incorrect post titles
   before they are published
 - Proper titles for category RSS feeds
 - Absolute URLs for RSS feeds (and maybe everywhere)
 - Test that follows all internal links to make sure they're correct

