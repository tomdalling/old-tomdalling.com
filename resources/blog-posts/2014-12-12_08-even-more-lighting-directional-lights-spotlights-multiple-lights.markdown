{:title "Modern OpenGL 08 – Even More Lighting: Directional Lights, Spotlights, & Multiple Lights"
 :disqus-id "com.tomdalling.modern-opengl-series.08"
 :main-image {:uri "/images/posts/modern-opengl-08.png"}
 :draft true
 :category :modern-opengl}

In this article, we will be adding directional lights, spotlights, and allowing
for multiple lights instead of just one. This is the final article on lighting
– at least for a while.

<!--more-->

Accessing The Code
------------------

Download all lessons as a zip from
here:[https://github.com/tomdalling/opengl-series/archive/master.zip][] 

Setup instructions are available in the first article: [Getting Started in
Xcode, Visual C++, and Linux][].

This article builds on the code from the previous article.

All the code in this series of articles is available from github:
[https://github.com/tomdalling/opengl-series][]. You can download a zip of all
the files from that page, or you can clone the repository if you are familiar
with git. The code for this article can be found in the
[`windows/08_even_more_lighting`][], [`osx/08_even_more_lighting`][], and
[`linux/08_even_more_lighting`][] directories.

Directional Lights
------------------

Directional lights are lights that shine in a single, uniform direction. That
is, all rays of light are parallel to each other. Pure directional lights do
not exist (except maybe lasers?) but they are often used in computer graphics
to imitate strong light sources that are very far away, such as the Sun. The
Sun radiates light in all directions, much like a point light. However, over an
enormous distance, the tiny fraction of light rays that make it to earth appear
to be almost parallel.

As we saw in the previous article on homogenous coordinates, directional lights
can be thought of as point lights that are infinitely far away. This causes an
unfortunate interaction with attenuation. Attenuation is the reduction of light
intensity over distance – the greater the distance, the dimmer the light is. If
there is even the tiniest amount of attenuation, over an infinite distance the
light becomes infinitely dim (a.k.a. invisible). For this reason, directional
lights are implemented such that they ignore attenuation. This kind-of makes
sense if we're using directional lights to represent the Sun, because the
sunlight that hits earth doesn't appear to have any attenuation.

The direction of a directional light can be determined by it's position. If it
is infinitely far away down the positive X axis $$(1, 0, 0, 0)$$, then it will
shine down the negative X axis $$(-1, 0, 0)$$.

[https://github.com/tomdalling/opengl-series/archive/master.zip]: https://github.com/tomdalling/opengl-series/archive/master.zip
[Getting Started in Xcode, Visual C++, and Linux]: http://tomdalling.com/blog/modern-opengl/01-getting-started-in-xcode-and-visual-cpp/
[https://github.com/tomdalling/opengl-series]: https://github.com/tomdalling/opengl-series
[`windows/08_even_more_lighting`]: https://github.com/tomdalling/opengl-series/tree/master/windows/08_even_more_lighting
[`osx/08_even_more_lighting`]: https://github.com/tomdalling/opengl-series/tree/master/osx/08_even_more_lighting
[`linux/08_even_more_lighting`]: https://github.com/tomdalling/opengl-series/tree/master/linux/08_even_more_lighting
