{:title "Explaining Homogenous Coordinates & Projective Geometry"
 :disqus-id "1388 http://tomdalling.com/?p=1388"
 :main-image {:uri "/images/posts/explaining-homogenous-coordinates/main-image.png"
              :artist "Joachim Baecker"
              :artist-url "http://commons.wikimedia.org/wiki/File:Zentralprojektion.jpg"}
 :category :modern-opengl}

In this article I'm going to explain homogenous coordinates (a.k.a. 4D
coordinates) as simply as I can. In previous articles, we've used 4D vectors
for matrix multiplication, but I've never really defined what the fourth
dimension actually is. Now it's time to take a closer look at projective
geometry.

Also, welcome back! It has been a while since my last post. Hopefully I will
find some time in the next couple of months to finish up the [Modern OpenGL
Series][] of articles. The code for article 08 is done, but writing the article
will take some time.

<!--more-->

Terminology
-----------

Most of the time when working with 3D, we are thinking in terms of Euclidean
geometry – that is, coordinates in three-dimensional space ([math]X[/math],
[math]Y[/math], and [math]Z[/math]). However, there are certain situations
where it is useful to think in terms of [projective geometry][] instead.
Projective geometry has an extra dimension, called [math]W[/math], in addition
to the [math]X[/math], [math]Y[/math], and [math]Z[/math] dimensions.  This
four-dimensional space is called "projective space," and coordinates in
projective space are called "homogenous coordinates."

For the purposes of 3D software, the terms "projective" and "homogenous" are
basically interchangeable with "4D."

Not Quaternions
---------------

Quaternions look a lot like homogenous coordinates. Both are 4D vectors,
commonly depicted as [math]\(X, Y, Z, W)[/math]. However, quaternions and
homogenous coordinates are different concepts, with different uses.

The contents of this article don't apply to quaternions. If I can find the
time, I might write a quaternion article in the future.

An Analogy In 2D
----------------

First, let's look at how projective geometry works in 2D, before we move on to
3D.

Imagine a projector that is projecting a 2D image onto a screen. It's easy to
identify the [math]X[/math] and [math]Y[/math] dimensions of the projected
image:

<figure class="nopadding">
  <img src="/images/posts/explaining-homogenous-coordinates/projector_xy.png" />
</figure>

<blockquote class="pull-right">
  The [math]W[/math] dimension is the distance from the projector to the
  screen.
</blockquote>

Now, if you step back from the 2D image and look at the projector and the
screen, you can see the [math]W[/math] dimension too. The [math]W[/math]
dimension is the distance from the projector to the screen.

<figure class="nopadding">
  <img src="/images/posts/explaining-homogenous-coordinates/projector_xyw.png" />
</figure>

<blockquote class="pull-right">
  The value of [math]W[/math] affects the size (a.k.a. scale) of the image.
</blockquote>

So what does the [math]W[/math] dimension do, exactly? Imagine what would
happen to the 2D image if you increased or decreased [math]W[/math] – that is,
if you increased or decreased the distance between the projector and the
screen. If you move the projector *closer* to the screen, the whole 2D image
becomes *smaller*. If you move the projector *away* from the screen, the 2D
image becomes *larger*. As you can see, the value of [math]W[/math] affects the
size (a.k.a. scale) of the image.

<figure class="nopadding">
  <img src="/images/posts/explaining-homogenous-coordinates/projector_close.png" />
</figure>

Applying It To 3D
-----------------

There is no such thing as a 3D projector (yet), so it's harder to imagine
projective geometry in 3D, but the [math]W[/math] value works exactly the same
as it does in 2D. When [math]W[/math] increases, the coordinate expands (scales
up). When [math]W[/math] decreases, the coordinate shrinks (scales down). The
[math]W[/math] is basically a scaling transformation for the 3D coordinate.

When W = 1
----------

The usual advice for 3D programming beginners is to always set [math]W =
1[/math] whenever converting a 3D coordinate to a 4D coordinate. The reason for
this is that when you scale a coordinate by [math]1[/math] it doesn't shrink or
grow, it just stays the same size. So, when [math]W = 1[/math] it has no effect
on the [math]X[/math], [math]Y[/math] or [math]Z[/math] values.

For this reason, when it comes to 3D computer graphics, coordinates are said to
be "correct" only when [math]W = 1[/math]. If you rendered coordinates with
[math]W > 1[/math] then everything would look too small, and with [math]W <
1[/math] everything would look too big. If you tried to render with [math]W =
0[/math] your program would crash when it attempted to divide by zero. With
[math]W < 0[/math] everything would flip upside-down and back-to-front.

Mathematically speaking, there is no such thing as an "incorrect" homogenous
coordinate. Using coordinates with [math]W = 1[/math] is just a useful
convention for 3D computer graphics.

The Math
--------

Now, let's look at some actual numbers, to see how the math works.

Let's say that the projector is [math]3[/math] meters away from the screen, and
there is a dot on the 2D image at the coordinate [math]\(15,21)[/math]. This
gives us the projective coordinate vector [math]\(X,Y,W) = (15,21,3)[/math].

<figure class="nopadding">
  <img src="/images/posts/explaining-homogenous-coordinates/projector_w_3.png" />
</figure>

Now, imagine that the projector was pushed closer to the screen so that the
distance was [math]1[/math] meter. The closer the project gets to the screen,
the smaller the image becomes. The projector has moved three times closer, so
the image becomes three times smaller. If we take the original coordinate
vector and divide all the values by three, we get the new vector where [math]W
= 1[/math]:

<figure>
  [blockmath]
    (\frac{15}{3}, \frac{21}{3}, \frac{3}{3}) = (5, 7, 1)
  [/blockmath]
</figure>

The dot is now at coordinate [math]\(5,7)[/math].

<figure class="nopadding">
  <img src="/images/posts/explaining-homogenous-coordinates/projector_w_1.png" />
</figure>

This is how an "incorrect" homogenous coordinate is converted to a "correct"
coordinate: divide all the values by [math]W[/math]. The process is exactly the
same for 2D and 3D coordinates.

Dividing all the values in a vector is done by scalar multiplication with the
reciprocal of the divisor. Here is a 4D example:

<figure>
  [blockmath]
    \frac{1}{5} (10, 20, 30, 5) = (\frac{10}{5}, \frac{20}{5}, \frac{30}{5}, \frac{5}{5}) = (2,4,6,1)
  [/blockmath]
</figure>

Written in C++ using GLM, The example above would look like this:

```cpp
glm::vec4 coordinate(10, 20, 30, 5);
glm::vec4 correctCoordinate = (1.0/coordinate.w) * coordinate;
//now, correctCoordinate == (2,4,6,1) 
```

Uses Of Homogenous Coordinates In Computer Graphics
---------------------------------------------------

As mentioned earlier, in regard to 3D computer graphics, homogenous coordinates
are useful in certain situations. We will look at some of those situations
here.

### Translation Matrices For 3D Coordinates

<blockquote class="pull-right">
  A four-column matrix can only be multiplied with a four-element vector, which
  is why we often use homogenous 4D vectors instead of 3D vectors.
</blockquote>

Rotation and scaling transformation matrices only require three columns. But,
in order to do translation, the matrices need to have at least four columns.
This is why transformations are often 4x4 matrices. However, a matrix with four
columns can not be multiplied with a 3D vector, due to the rules of matrix
multiplication. A four-column matrix can only be multiplied with a four-element
vector, which is why we often use homogenous 4D vectors instead of 3D vectors.

The 4<sup>th</sup> dimension [math]W[/math] is usually unchanged, when using
homogenous coordinates in matrix transformation. [math]W[/math] is set to
[math]1[/math] when converting a 3D coordinate into 4D, and it is usually still
[math]1[/math] after the transformation matrices are applied, at which point it
can be converted back into a 3D coordinate by ignoring the [math]W[/math]. This
is true for all translation, rotation, and scaling transformations, which are
by far the most common types of transformations. The notable exception is
projection matrices, which do affect the [math]W[/math] dimension.

### Perspective Transformation

In 3D, "perspective" is the phenomenon where an object appears smaller the
further away it is from the camera. A far-away mountain can appear to be
smaller than a cat, if the cat is close enough to the camera.

<figure class="nopadding">
  <img src="/images/posts/explaining-homogenous-coordinates/cat_vs_mountain.jpg" />
</figure>

<blockquote class="pull-right">
  Perspective is implemented in 3D computer graphics by using a transformation
  matrix that changes the [math]W[/math] element of each vertex.
</blockquote>

Perspective is implemented in 3D computer graphics by using a transformation
matrix that changes the [math]W[/math] element of each vertex. After the the
camera matrix is applied to each vertex, but before the projection matrix is
applied, the [math]Z[/math] element of each vertex represents the distance away
from the camera. Therefor, the larger [math]Z[/math] is, the more the vertex
should be scaled down. The [math]W[/math] dimension affects the scale, so the
projection matrix just changes the [math]W[/math] value based on the
[math]Z[/math] value. Here is an example of a perspective projection matrix
being applied to a homogenous coordinate:

<figure>
  [blockmath]
    \begin{bmatrix}
      1 & 0 & 0 & 0 \\\\
      0 & 1 & 0 & 0 \\\\
      0 & 0 & 1 & 0 \\\\
      0 & 0 & 1 & 0
    \end{bmatrix}
    \begin{bmatrix} 2 \\\\ 3 \\\\ 4 \\\\ 1 \end{bmatrix} =
    \begin{bmatrix} 2 \\\\ 3 \\\\ 4 \\\\ 4 \end{bmatrix}
  [/blockmath]
</figure>

Notice how the [math]W[/math] value is changed to [math]4[/math], which comes
from the [math]Z[/math] value.

<blockquote class="pull-right">
  Perspective division is just a specific term for converting the homogenous
  coordinate back to [math]W = 1[/math]
</blockquote>

After the perspective projection matrix is applied, each vertex undergoes
"perspective division." Perspective division is just a specific term for
converting the homogenous coordinate back to [math]W = 1[/math], as explained
earlier in the article. Continuing with the example above, the perspective
division step would look like this:

<figure>
  [blockmath]
    \frac{1}{4} (2, 3, 4, 4) = (0.5, 0.75, 1, 1)
  [/blockmath]
</figure>

After perspective division, the [math]W[/math] value is discarded, and we are
left with a 3D coordinate that has been correctly scaled according to a 3D
perspective projection.

In GLM, this perspective projection matrix can be created using the
`glm::perspective` or `glm::frustum` functions. In old-style OpenGL, it is
commonly created with the `gluPerspective` or `gluFrustum` functions. In
OpenGL, perspective division happens automatically after the vertex shader runs
on each vertex. This is one reason why `gl_Position`, the main output of the
vertex shader, is a 4D vector, not a 3D vector.

### Positioning Directional Lights

One property of homogenous coordinates is that they allow you to have points at
infinity (infinite length vectors), which is not possible with 3D coordinates.
Points at infinity occur when [math]W = 0[/math]. If you try and convert a
[math]W = 0[/math] homogenous coordinate into a normal [math]W = 1[/math]
coordinate, it results in a bunch of divide-by-zero operations:

<figure>
  [blockmath]
    \frac{1}{0} (2, 3, 4, 0) = (\frac{2}{0}, \frac{3}{0}, \frac{4}{0}, \frac{0}{0})
  [/blockmath]
</figure>

This means that homogenous coordinates with [math]W = 0[/math] can not be
converted into 3D coordinates.

What use does this have? Well, directional lights can be though of as point
lights that are infinitely far away. When a point light is infinitely far away
the rays of light become parallel, and all of the light travels in a single
direction. This is basically the definition of a directional light.

<blockquote class="pull-right">
  If [math]W = 1[/math], then it is a point light. If [math]W = 0[/math], then
  it is a directional light.
</blockquote>

So traditionally, in 3D graphics, directional lights are differentiated from
point lights by the value of [math]W[/math] in the position vector of the
light. If [math]W = 1[/math], then it is a point light. If [math]W = 0[/math],
then it is a directional light. 

This is more of a traditional convention, rather than a useful way to write
lighting code. Directional lights and point lights are usually implemented with
separate code, because they behave differently. A typical lighting shader might
look like this:

```glsl
if(lightPosition.w == 0.0){
    //directional light code here
} else {
    //point light code here
}
```

Summary
-------

Homogenous coordinates have an extra dimension called [math]W[/math], which
scales the [math]X[/math], [math]Y[/math], and [math]Z[/math] dimensions.
Matrices for translation and perspective projection transformations can only be
applied to homogenous coordinates, which is why they are so common in 3D
computer graphics. The [math]X[/math], [math]Y[/math], and [math]Z[/math]
values are said to be "correct" when [math]W = 1[/math]. Any homogenous
coordinate can be converted to have [math]W = 1[/math] by dividing all four
dimensions by the [math]W[/math] value, except if [math]W = 0[/math]. When
[math]W = 0[/math], the coordinate represents a point at infinity (a vector
with infinite length), and this is often used to denote the direction of
directional lights.

[Modern OpenGL Series]: http://tomdalling.com/blog/category/modern-opengl/
[projective geometry]: http://en.wikipedia.org/wiki/Projective_geometry

