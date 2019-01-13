<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Pigeons Server</title>
    </head>
    <body>
        <h1>Pigeons Server is running...</h1>
        <hr />
	<h4>
		Build:<%=String.valueOf(application.getAttribute("build.version")) + "."
					+ String.valueOf(application.getAttribute("build.number")) + ". Date: "
					+ String.valueOf(application.getAttribute("build.date"))%>
	</h4>
    </body>
</html>
