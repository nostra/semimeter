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
    <li><a href="c/semimeter/pub1/person.gif">c/semimeter/pub1/person.gif</a></li>
    <li><a href="c/semimeter/pub1/search.gif">c/semimeter/pub1/search.gif</a></li>
    <li><a href="c/semimeter/pub2/search.gif">c/semimeter/pub2/search.gif</a></li>
    <li><a href="c/semimeter/pub2/person.gif">c/semimeter/pub2/person.gif</a></li>
</ul>
<br/>
<h1>Display aggregated data:</h1>
    Notice that the number will reflect the aggregated number, based upon sets and sub sets.
    <ul>
        <li><a href="show/semimeter/?resolution=total">show/semimeter/?resolution=total - totals under semimeter tree</a></li>
        <li><a href="show/semimeter/pub1?resolution=total">show/semimeter/pub1?resolution=total - totals under semimeter/pub1 </a></li>
        <li><a href="show/semimeter/pub1/search?resolution=total">show/semimeter/pub1/search?resolution=total - totals under semimeter/pub1/search</a></li>
        <li><a href="show/semimeter/pub1/?resolution=day">show/semimeter/pub1/?resolution=day - sum under sub tree with resolution last 24 hours</a></li>
        <li><a href="show/%25/search?resolution=total">show/%25/search?resolution=total sum up <i>all</i> trees which contain <i>/search</i> as partial contents.</a></li>
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

<div class="box">
<h1>Displaying graph based in JSON query</h1>
    As an example, a graph tool from yahoo has been included. It reads JSON data. The
    data use is geared towards returning an array.
    <br/><br/>
    JSON examples:
<ul>
    <li><a href="semimeter/$/search/json.html?resolution=hour">semimeter/$/search/json.html?resolution=hour - fill in the blank between <i>semimeter</i> and <i>search</i>, and display count</a></li>
    <li><a href="semimeter/$/json.html?resolution=hour">semimeter/$/json.html?resolution=hour - everything that starts with semimeter</a></li>
</ul>
    <br/><br/>
    Usage of JSON in yahoo graph:
<ul>
    <li><a href="semimeter/$/search/graph.html?resolution=hour">semimeter/$/search/graph.html?resolution=hour - fill in the blank between <i>semimeter</i> and <i>search</i>, and display count</a></li>
    <li><a href="semimeter/$/graph.html?resolution=hour">semimeter/$/graph.html?resolution=hour - everything that starts with semimeter</a></li>
</ul>
</div>
<br />

<!-- Include own statistics as example -->
<img src="c/semimeter/index.gif" alt="" />
</body>
</html>