这是一个XML的Diff/Merge工具，可以用来做git的merge driver。

原工程在 [这里](http://tdm.berlios.de/3dm/doc/index.html)

我改了一版，可以在Windows + cygwin下运行。

* 生成patch文件：

      3dm -d <base> <branch1> [output]

* 应用patch文件：

      3dm -p <base> <patch> [output]

* 3-way merge:

      3dm -m <base> <branch1> <branch2> [output]
