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
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title>Live search statistics</title>
    <meta http-equiv="Content-type" content="text/html; charset=UTF-8">
    <link href="<c:url value="/css/style.css"/>" rel="stylesheet" type="text/css" title="SemiMeter">
    <script type="text/javascript" src="http://yui.yahooapis.com/combo?2.8.0r4/build/utilities/utilities.js&2.8.0r4/build/datasource/datasource-min.js&2.8.0r4/build/autocomplete/autocomplete-min.js&2.8.0r4/build/json/json-min.js&2.8.0r4/build/swf/swf-min.js&2.8.0r4/build/charts/charts-min.js&2.8.0r4/build/cookie/cookie-min.js&2.8.0r4/build/datemath/datemath-min.js&2.8.0r4/build/element-delegate/element-delegate-min.js&2.8.0r4/build/selector/selector-min.js&2.8.0r4/build/event-delegate/event-delegate-min.js&2.8.0r4/build/event-mouseenter/event-mouseenter-min.js&2.8.0r4/build/event-simulate/event-simulate-min.js&2.8.0r4/build/history/history-min.js&2.8.0r4/build/imageloader/imageloader-min.js&2.8.0r4/build/resize/resize-min.js&2.8.0r4/build/layout/layout-min.js&2.8.0r4/build/swfstore/swfstore-min.js&2.8.0r4/build/storage/storage-min.js&2.8.0r4/build/stylesheet/stylesheet-min.js"></script>


    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/fonts/fonts-min.css" />
    <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.8.0r4/build/datatable/assets/skins/sam/datatable.css" />
    <script type="text/javascript" src="http://yui.yahooapis.com/2.8.0r4/build/yahoo-dom-event/yahoo-dom-event.js"></script>

    <script type="text/javascript" src="http://yui.yahooapis.com/2.8.0r4/build/dragdrop/dragdrop-min.js"></script>
    <script type="text/javascript" src="http://yui.yahooapis.com/2.8.0r4/build/element/element-min.js"></script>
    <script type="text/javascript" src="http://yui.yahooapis.com/2.8.0r4/build/datasource/datasource-min.js"></script>
    <script type="text/javascript" src="http://yui.yahooapis.com/2.8.0r4/build/datatable/datatable-min.js"></script>

    <style type="text/css">
        #chart {
            width: 100%;
            height: <c:choose><c:when test="${!empty param.gh}">${param.gh}px;</c:when><c:otherwise>600px;</c:otherwise></c:choose>
            float:left;
        }
    </style>
</head>

<body class="yui-skin-sam">
<div id="header">
    <div id="header-top">
        <a href="<c:url value="/"/>"><img src="<c:url value="/gfx/logo.png"/>" width="150" height="30" alt="logo" /></a>
    </div>
    <div id="header-bottom">
        Live search statistics 
    </div>
</div>
<div id="main">


    <div id="data">
        <div id="chart"></div>
        <!--<div id="numbers"></div>-->
    </div>
    <div id="disclaimer">
        The graph is automatically updated every <fmt:formatNumber value="${updateInterval / 1000}" pattern="####"/> second.
    </div>

    <script type="text/javascript">
        
        YAHOO.widget.Chart.SWFURL = "http://yui.yahooapis.com/2.8.0r4/build/charts/assets/charts.swf";

        var jsonData = new YAHOO.util.DataSource("json.html?resolution=${resolution}");
        jsonData.connMethodPost = true;
        jsonData.responseType = YAHOO.util.DataSource.TYPE_JSON;
        jsonData.responseSchema = { resultsList: "Results", fields: [ "key", "value" ] };

        var seriesDef =
                [
                    {
                        xField: "value",
                        displayName: "searches",
                        style:
                        {
                            color: 0xff6600,
                            size: 10
                        }

                    }
                ];

        var myAxis = new YAHOO.widget.NumericAxis();
        myAxis.minimum = 0;
        myAxis.maximum = <c:out value="${xAxisSize}" default="10" escapeXml="false" />;

        var mychart = new YAHOO.widget.StackedBarChart("chart", jsonData,
        {
            series: seriesDef,
            yField: "key",
            xAxis: myAxis,
            //polling: ${updateInterval},
            polling: 1000,
            style:
            {
                border: {color: 0x999999, size: 1},
                font: {name: "Arial Black", size: 10, color: 0x586b71},
                dataTip:
                {
                    border: {color: 0x2e434d, size: 2},
                    font: {name: "Arial Black", size: 10, color: 0x586b71}
                },
                xAxis:
                {
                    color: 0x2e434d
                },
                yAxis:
                {
                    color: 0x2e434d,
                    majorTicks: {color: 0x2e434d, length: 4},
                    minorTicks: {color: 0x2e434d, length: 2},
                    majorGridLines: {size: 0}
                }
            }
        });

    </script>

</div>
</body>
</html>