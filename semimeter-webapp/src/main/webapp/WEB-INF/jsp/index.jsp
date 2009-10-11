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
    <h1>SemiMeter retains statistics generated from calls on blank gif. </h1><br/>This page also calls this gif, in order
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
        <li><a href="show/semimeter/?resolution=total">show/semimeter/?resolution=total - totals under semimeter tree</a></li>
        <li><a href="show/semimeter/a?resolution=total">show/semimeter/a?resolution=total - totals under semimeter/a </a></li>
        <li><a href="show/semimeter/a/b/?resolution=total">show/semimeter/a/b/?resolution=total - sum under sub tree</a></li>
        <li><a href="show/semimeter/a/b/?resolution=total">show/semimeter/a/b/?resolution=day - sum under sub tree with resolution last 24 hours</a></li>
        <li><a href="show/%25/a/?resolution=total">show/%25/a/?resolution=total sum up <i>all</i> trees which contain <i>/a/</i> as partial contents.</a></li>
        <li><a href="change/semimeter?resolution=day">change/semimeter?resolution=day - sum up the difference between current 24 hours, and the previous 24 hours</a></li>
    </ul>
    The resolution can be <b>second</b>, <b>minute</b>, <b>hour</b>, <b>day</b>, <b>week</b>, <b>month</b>, or
    <b>total</b>. The last is <i>all</i> available data. It is also the default value, which is also used
    if the resolution parameter is wrongly stated. Notice that the time is skewed with 30 seconds, as we do not
    try to provide real real-time data, but something practically close.
    <br/><br/>
    Notice that querying for change when having resolution <i>total</i> does not really make sense.
</div>
<br />
<!-- Include own statistics as example -->
<img src="c/semimeter/index.gif" alt="" />
</body>
</html>