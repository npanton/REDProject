var map, json, geocoder;
var shapeArray = [], prettyLocation = {}, markerArray = [], shapeArrayAlt = [];

$(document).ready(function() {
	
    geocoder = new google.maps.Geocoder();
    var latlng = new google.maps.LatLng(53.5, -1.6);
    var myOptions = {
        zoom: 8,
        center: latlng,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map = new google.maps.Map(document.getElementById("map_canvas"), myOptions);
    getTree();

    var goToRoot = document.createElement('DIV');
    goToRoot.style.padding = '5px';
    goToRoot.style.border = '1px solid #000';
    goToRoot.style.backgroundColor = 'white';
    goToRoot.style.cursor = 'pointer';
    goToRoot.innerHTML = 'Go To Root';
    goToRoot.index = 1;
    
    var clusterData = document.createElement('DIV');
    clusterData.style.padding = '5px';
    clusterData.style.border = '1px solid #000';
    clusterData.style.backgroundColor = 'white';
    clusterData.style.cursor = 'pointer';
    clusterData.innerHTML = 'Cluster Data';
    clusterData.index = 1;
    
    loadTimeline();
    google.maps.event.addDomListener(goToRoot, 'click', function() {
	var latlng = new google.maps.LatLng(53.5, -1.6);
        map.setCenter(latlng);
        map.setZoom(8);
        st.onClick(json["id"]);
        clearMapShapes();
        clearMapMarkers();
        drawTree(json, 0.1, false);
    });
    
    google.maps.event.addDomListener(clusterData, 'click', function() {
            alert("Clustering!");
    });
    
    map.controls[google.maps.ControlPosition.TOP_RIGHT].push(goToRoot);
    map.controls[google.maps.ControlPosition.TOP_RIGHT].push(clusterData);


});

function dateToFile(date){
    return date.getDate() + "D" + (date.getMonth()+1) + "M" +date.getFullYear() + "Y"
}

function reDrawMap(){
    clearMapShapes();
    clearMapMarkers();
    map.setCenter(new google.maps.LatLng(53.5, -1.6));
    map.setZoom(8);
    var url = "tree/?id="+dateToFile(currDate)
    $.get(url, function (data){
        drawTree(data, 4, false);
        st.loadJSON(data);
        st.setRoot(data["id"], "animate", null);	
        json = data;
        //load json data

        findAddresses(data);
    });
    st.addSubtree(json, 'animate', null);
    st.setRoot(json["id"], 'animate', null);
	
}

function getTree(){
	
    $.get("tree/?id="+dateToFile(currDate), function (data){
//        data = jQuery.parseJSON(data);
        drawTree(data, 4, false);
        json = data;	
        findAddresses(data);
        initTree();
    });
}

function drawTree(data, depth, alt){
    var children = data["children"];
    // Alt now means draw 1 depth below
    if(alt){
        var shape = drawShape(data);
        
        
        for(var j = 0; j < children.length; j++){
            var child = children[j]	
            var shape = drawShape(child);
            shape.setOptions({zIndex: -1, fillColor : '#FFCC99', fillOpacity: 1});
            shape.setMap(map);
        shapeArrayAlt.push(shape);
        }
        
    }
    else{
        
        for(var j = 0; j < children.length; j++){
            var child = children[j]	
            // if(child["hull"].length > 2 && data["children"].length != 0){
            depth += 1
            var shape = drawShape(child);
            setShapeListener(shape, child);
				
            // }
            // else{
            // 	drawMarker(child);
            // }
        }
        // if(children.length == 0)
        // drawMarker(data);
		
    }
}

function drawMarker(node){
    for(var i = 0; i < node["data"].length; i++){
        $.get("tweet/?id="+node["data"][i], function (data){
            var marker = new google.maps.Marker({
                map: map,
                position: new google.maps.LatLng(data["lat"], data["lon"]),
                draggable: false
            });
            setMarkerListener(marker, data["tweet"]);
            markerArray.push(marker);
        });

    }
}

function setMarkerListener(marker, tweet){
    google.maps.event.addListener(marker, 'click', function() {
        var infowindow = new google.maps.InfoWindow({
            content: tweet
        });
        infowindow.open(map,marker);
    });
}

function drawShape(node){
    var path = [];
    for(var i = 0; i < node["hull"].length; i++){
        path.push(new google.maps.LatLng(node["hull"][i][2], node["hull"][i][3]));
    }
    var shape = new google.maps.Polygon({
        path: path,
        strokeColor: '#000000',
        strokeOpacity: 0.5,
        strokeWeight: 0.5,
//        fillColor: calcColour(node["high"], node["low"], node["size"]),
        fillColor: '#FFCC99',
        fillOpacity: 0.35
    });

return shape;
}

function setZoomByNode(nodeId){
    if(nodeId.substring(0, nodeId.indexOf("-")) == "Root"){
        map.setZoom(8);
    }
    else{
        map.setZoom(parseInt(nodeId.substring(0, nodeId.indexOf("-")))+7); 
    }	
}

function treeToMap(nodeId){
    if(json["id"] == nodeId){
        clearMapShapes();
        clearMapMarkers();
        drawTree(json, 0.1, true);
        map.setCenter(getLatLngCenter(json["hull"]));
        setZoomByNode(nodeId);
    }else{
        walk(nodeId, json);
    }
}

function walk(nodeId, data){
    var children = data["children"]
    for(var j = 0; j < children.length; j++){
        var child = children[j];
        if(child["id"] == nodeId){
            clearMapShapes();
            clearMapMarkers();
            drawTree(child, 0.1, true);
            map.setCenter(getLatLngCenter(child["hull"]));
            setZoomByNode(child["id"]);			
            return
        }
        walk(nodeId, child);
    }	
}

function findAddresses(data){
    // codeLatLng(getLatLngCenter(data["hull"]), data["id"]);
    var children = data["children"];
    for(var j = 0; j < children.length; j++){
        codeLatLng(getLatLngCenter(children[j]["hull"]), children[j]["id"]);
		
	// 	var child = children[j];
	// 	findAddresses(child);
    }
}

function findNode(nodeId){
    if(json["id"] == nodeId){
        return json;
    }else{
        walkFindNode(nodeId, json);
    }
}


function walkFindNode(nodeId, data){
    var children = data["children"]
    for(var j = 0; j < children.length; j++){
        var child = children[j];		
        if(child["id"] == nodeId){
            returnChild = child;
            return;
        }
        walk(nodeId, child);
		
    }	
}

function calcColour(high, low, size){
    var val = Math.round(((size/high)*6));
//    return ['#aaa', '#b99', '#c77', '#d66', '#e44', '#f22', '#f33'][val]; 	
return '#b99';
}

function clearMapShapes(){
    for(i in shapeArray){
        shapeArray[i].setMap(null);
    }
    shapeArray = [];
  

}
function clearMapShapesAlt(){
    for(i in shapeArrayAlt){
        shapeArrayAlt[i].setMap(null);
    }
    shapeArrayAlt = [];
  

}

function clearMapMarkers(){
    for(i in markerArray){
        markerArray[i].setMap(null);
    }
    markerArray = [];
}

function setShapeListener(shape, child){
    shapeArray.push(shape);
    shape.setMap(map);
    google.maps.event.addListener(shape, 'click', function() {
        map.setCenter(getPolygonCenter(shape));
        setZoomByNode(child["id"]);
        if(child["children"].length > 0)
            shape.setMap(null);
        drawTree(child, 0.1, false);
        st.onClick(child["id"]);
       
    });
    
    google.maps.event.addListener(shape, 'mouseover', function() {
        shape.setOptions({fillColor: '#000000'});
        drawTree(child, 0.1, true);
    });
    google.maps.event.addListener(shape, 'mouseout', function() {
        shape.setOptions({fillColor: '#FFCC99'});
        clearMapShapesAlt();
    });

}




function getLatLngCenter(latlng) {
    var n = latlng.length;
    var lat = 0, lng = 0;
    for(var i = 0; i < n; i++) {
        var vert = latlng[i];
        lat += vert[2];
        lng += vert[3];
    }
    return new google.maps.LatLng(lat / n, lng / n);
}

function codeLatLng(latlng, id) {
    geocoder.geocode( { 'latLng': latlng}, function(results, status) {
        if (status == google.maps.GeocoderStatus.OK) {
            // alert(results[0].formatted_address);
            // map.setCenter(results[0].geometry.location);
            // var marker = new google.maps.Marker({
            // 	map: map, 
            // 	position: results[0].geometry.location
            // });
            var address = "";
			
            var addressDetails = results[0].address_components;
            for (var i=0; i < addressDetails.length; i++) {
                var inner = addressDetails[i].types;
                for (var j=0; j < inner.length; j++) {
                    if(inner[j].toLowerCase() == "sublocality" || inner[j].toLowerCase() == "locality" || inner[j].toLowerCase() == "administrative_area_level_1"){
                        address += " " + addressDetails[i].long_name;
                    }
					
                }
            }
            // prettyLocation[id] = results[0].formatted_address;
            if (address != "")
                prettyLocation[id] = address;
            else
                prettyLocation[id] = "USA";
			
        } else {
            prettyLocation[id] = "USA";
        }
    });
}


function getPolygonCenter(poly) {
    var n = poly.getPath().getLength();
    var lat = 0, lng = 0;
    for(var i = 0; i < n; i++) {
        var vert = poly.getPath().getAt(i);
        lat += vert.lat();
        lng += vert.lng();
    }
    return new google.maps.LatLng(lat / n, lng / n);
}