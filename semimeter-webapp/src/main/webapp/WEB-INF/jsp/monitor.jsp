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
    <title>SemiMeter Monitor - ${path}</title>
    <meta http-equiv="Content-type" content="text/html; charset=UTF-8">

    <link href="<c:url value="/css/monitor.css"/>" rel="stylesheet" type="text/css" title="SemiMeter">
    <script type="text/javascript" src="<c:url value="/js/jquery-1.3.2.min.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/js/jquery.timers-1.2.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/js/jquery-ui-1.7.2.custom.min.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/js/semimeter.js"/>"></script>

    <script type="text/javascript">

        function updateStatistics() {
            $.ajax({
               type: "GET",
               url: "<c:url value="/show${path}?resolution=${counterresolution}"/>",
               dataType: "json",
               cache: false,
               success: function(data1){
                   $.each(data1.Results, function(i, item1) {
                       if (fNum(item1.value) != $("#theNumber").html()) {
                           $("#theNumber").fadeOut(500, function() {
                               doUpdateStatistics(item1.value);
                           });
                       }
                       if (i == 1) return true;
                   });
               },
               error: function(XMLHttpRequest, textStatus, errorThrown) {
                   // Keep old number if possible 
                   //$("#theNumber").text("please wait...");
               }
             });
        }

        function doUpdateStatistics(s) {
            $("#theNumber").text(fNum(s));
            $("#theNumber").fadeIn(500);
        }


        function updateGraph() {
            $.ajax({
                type: "GET",
                url: "<c:url value="${path}/array.html?resolution=${graphresolution}&numberOfSamples=${graphsamples}"/>",
                dataType: "json",
                cache: false,
                success: function(data) {
                    var str = "";
                    var max = 0;

                    $.each(data.Results, function(i, item) {
                        str += item.value + ",";
                        if (parseInt(item.value) > max) {
                            max = parseInt(item.value);
                        }
                    });

                    if (str != "") {
                        str = str.substr(0, str.length - 1);
                        var chartUrl = "http://chart.apis.google.com/chart?cht=lc&chs=668x135&chd=t:" + str +
                                       "&chxr=0,0," + max + "&chds=0," + max + "&chxt=y&chxl=";

                        if ($("#chartBarImg").attr("src") != chartUrl) {
                            $("#chartBarImg").attr("src", chartUrl);
                        }
                    }
                    return true;
                },
                error: function(XMLHttpRequest, textStatus, errorThrown) {
                    // Keep old image if any
                }
            });
        }

        $(document).ready(function() {
            $(document).everyTime(6000, function(i) {
                updateStatistics();
            });

            $(document).everyTime(6000, function(i) {
                updateGraph();
            });

            $("#disclaimerButton").click(function() {
                $("#disclaimer").toggle("blind");
                return false;
            });

            updateStatistics();
            updateGraph();
        });

    </script>

</head>
<body>

<div id="mainContent">
    <div id="disclaimer">
        <h1>SemiMeter monitor</h1>

        <p>The information on this page is updated every 6 second.</p>

    </div>
    <div id="infoBar">
        <div id="disclaimerButton"><img src="<c:url value="/gfx/info.png"/>" alt=""></div>
        SemiMeter statistics for : ${path}

    </div>
    <div id="numberBar">
        <div id="numberBarTopShadow"></div>
        <div id="numberBarMain"><p id="theNumber"></p></div>
        <div id="numberBarBottomShadow"></div>
    </div>

    <div id="chartBar">
        <div id="chart"><img id="chartBarImg" src="<c:url value="/gfx/blank.gif"/>" alt="" /></div>
    </div>
</div>

</body>
</html>