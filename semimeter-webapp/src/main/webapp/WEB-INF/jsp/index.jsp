<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<%--
  ~ Copyright 2009 Erlend Nossum
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~  http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
     <link href="css/style.css" rel="stylesheet" type="text/css" title="SemiMeter">
    <title>SemiMeter</title>
</head>
<body>
<div class="boxtop">
    SemiMeter retains statistics generated from calls on blank gif. <br/><br/>This page also calls this gif, in order
    to provide an example.
</div>
<div class="boxbottom">
<h1>Registration of data</h1>
The following links are example of URLs that can be used in a img src expression.
<ul>
    <li><a href="c/semimeter/a.gif">c/semimeter/a.gif</a></li>
    <li><a href="c/semimeter/a/b.gif">c/semimeter/a/b.gif</a></li>
    <li><a href="c/semimeter/a/b/c.gif">c/semimeter/a/b/c.gif</a></li>
    <li><a href="c/semimeter/a/b/c/d.gif">c/semimeter/a/b/c/d.gif</a></li>
</ul>
<br/>
<h1>Display aggregated data:</h1>
    Notice that the number will reflect the aggregated number, based upon sets and sub sets.
    <ul>
        <li><a href="showfeed/semimeter/">showfeed/semimeter/ - totals under semimeter tree</a></li>
        <li><a href="showfeed/semimeter/a">showfeed/semimeter/a - totals under semimeter/a </a></li>
        <li><a href="showfeed/semimeter/a/b/">showfeed/semimeter/a/b/ - </a></li>
    </ul>
</div>
<br />
<!-- Include own statistics as example -->
<img src="c/semimeter/index.gif" alt="" />
</body>
</html>