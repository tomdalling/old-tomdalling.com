" html -> markdown helper macros
let @p = 'df"i```f"df>i/\/pre€kldf>a```' " replaces <pre lang='blah'> with ```blah code block
let @o = 'f>i lang="cpp"F<@p' "like @p, but puts a 'lang=cpp' attr into the pre first
let @l = 'dst<<I -  0' " replace <li> with markdown list item
let @s = 'cf>**/\/strong€klcf>**' " replace <strong> with **
let @e = 'cst*' " replace <em> with *
let @a = 'd/"x"zd/"d/\v\>x"xd/\v\<\/adf>€kla["xpa][]mmGo["xpa]: "zpa`m' "replace <a> link with markdown link
let @h = 'dstyyp:s/./-/€kd' "turn <h2> into markdown h2
let @b = 'f>a	$F<i€kb€kugqq'
