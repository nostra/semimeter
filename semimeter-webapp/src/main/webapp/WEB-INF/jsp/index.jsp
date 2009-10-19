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

<div id="header">
    <div id="header-top">
        <a href="<c:url value="/"/>"><img src="<c:url value="/gfx/logo.png"/>" width="150" height="30" alt="logo" /></a>
    </div>
    <div id="header-bottom">
        SemiMeter retains statistics generated from calls on blank gif. 
    </div>
</div>
<div id="main">

<h2>Registration of data</h2>
The following links are example of URLs that can be used in a img src expression.
<ul>
    <li><a href="c/semimeter/pub1/person.gif">c/semimeter/pub1/person.gif</a></li>
    <li><a href="c/semimeter/pub1/search.gif">c/semimeter/pub1/search.gif</a></li>
    <li><a href="c/semimeter/pub2/search.gif">c/semimeter/pub2/search.gif</a></li>
    <li><a href="c/semimeter/pub2/person.gif">c/semimeter/pub2/person.gif</a></li>
</ul>

<h2>Display aggregated data:</h2>
    Notice that the number will reflect the aggregated number, based upon sets and sub sets.
    <ul>
        <li><a href="show/semimeter/?resolution=total">show/semimeter/?resolution=total - totals under semimeter tree</a></li>
        <li><a href="show/semimeter/pub1?resolution=total">show/semimeter/pub1?resolution=total - totals under semimeter/pub1 </a></li>
        <li><a href="show/semimeter/pub1/search?resolution=total">show/semimeter/pub1/search?resolution=total - totals under semimeter/pub1/search</a></li>
        <li><a href="show/semimeter/pub1/?resolution=day">show/semimeter/pub1/?resolution=day - sum under sub tree with resolution last 24 hours</a></li>
        <li><a href="show/%25/search?resolution=total">show/%25/search?resolution=total sum up <i>all</i> trees which contain <i>/search</i> as partial contents.</a></li>
        <li><a href="change/semimeter?resolution=day">change/semimeter?resolution=day - sum up the difference between current 24 hours, and the previous 24 hours</a></li>
    </ul>

    <p>The resolution can be <b>second</b>, <b>minute</b>, <b>hour</b>, <b>day</b>, <b>week</b>, <b>month</b>, or
    <b>total</b>. The last is <i>all</i> available data. It is also the default value, which is also used
    if the resolution parameter is wrongly stated. Notice that the time is skewed with 30 seconds, as we do not
    try to provide real real-time data, but something practically close.</p>

    <p>Notice that querying for change when having resolution <i>total</i> does not really make sense.</p>

    <h2>Displaying graph based in JSON query</h2>
    <p>As an example, a graph tool from yahoo has been included. It reads JSON data. The
    data use is geared towards returning an array.</p>


    <h3>JSON examples:</h3>
    <ul>
        <li><a href="semimeter/$/search/json.html?resolution=hour">semimeter/$/search/json.html?resolution=hour - fill in the blank between <i>semimeter</i> and <i>search</i>, and display count</a></li>
        <li><a href="semimeter/$/json.html?resolution=hour">semimeter/$/json.html?resolution=hour - everything that starts with semimeter</a></li>
    </ul>

    <h3>Using JSON in yahoo graph:</h3>
    <ul>
        <li><a href="semimeter/$/search/graph.html?resolution=hour">semimeter/$/search/graph.html?resolution=hour - fill in the blank between <i>semimeter</i> and <i>search</i>, and display count</a></li>
        <li><a href="semimeter/$/graph.html?resolution=hour">semimeter/$/graph.html?resolution=hour - everything that starts with semimeter</a></li>
    </ul>


    <h3>Querying for array:</h3>
    If you want to create a graph of the elements on a scale, the array query can be used.
    <ul>
        <li><a href="semimeter/pub1/search/array.html?resolution=day&amp;numberOfSamples=24">semimeter/pub1/search/array.html?resolution=day&amp;numberOfSamples=10 - Create an array with a length of 24 with basis in the last day, and display the number of samples in the relative slots. As the number of samples is 24, you get a number for each hour.</a></li>
        <li><a href="semimeter/array.html?resolution=minute&amp;numberOfSamples=60">semimeter/array.html?resolution=minute&amp;numberOfSamples=60 - Query for an array with data from the last minute. As the number of samples is specified to be 60, you get values for each second.</a></li>
    </ul>

    <h3>Data presentation on page:</h3>
    <ul>
        <li><a href="semimeter/pub1/monitor.html">semimeter/pub1/monitor.html - monitor subtree of semimeter/pub1.</a></li>
        <li><a href="monitor.html">monitor.html - display a grand total.</a></li>
        <li><a href="monitor.html?graphresolution=hour">monitor.html?graphresolution=hour - display total last hour.</a></li>
        <li><a href="monitor.html?counterresolution=minute">monitor.html ?counterresolution=minute- display a grand total in graph, whilst using minutes as resolution in counter.</a></li>
        <li><a href="monitor.html?graphresolution=month&amp;counterresolution=total">monitor.html?graphresolution=month&amp;counterresolution=total - display monthly data in graph, whilst showing total in counter.</a></li>
    </ul>

    <hr/>
    <p>
        This page also calls this gif, in order to provide an example.
    </p>

<!-- Include own statistics as example -->
<img src="c/semimeter/index.gif" alt="" />
</div>
</body>
</html>