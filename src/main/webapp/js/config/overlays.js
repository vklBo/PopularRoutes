var myStyle = [];

myStyle[1] = {
	"color" : "#A9D0F5",
	"weight" : 2,
	"opacity" : 0.85
};
myStyle[2] = {
	"color" : "#81BEF7",
	"weight" : 2,
	"opacity" : 0.85
};
myStyle[3] = {
	"color" : "#58ACFA",
	"weight" : 2,
	"opacity" : 0.85
};
myStyle[4] = {
	"color" : "#0080FF",
	"weight" : 2,
	"opacity" : 0.85
};
myStyle[5] = {
	"color" : "#0174DF",
	"weight" : 2,
	"opacity" : 0.85
};
myStyle[6] = {
	"color" : "#045FB4",
	"weight" : 2,
	"opacity" : 0.85
};
myStyle[7] = {
	"color" : "#084B8A",
	"weight" : 2,
	"opacity" : 0.85
};

var ol = [];
var visible = [];

for (i = 1; i <= 7; ++i) {
	ol[i] = L.geoJSON([ {
		"type" : "MultiLineString",
		"coordinates" : []
	} ], {
		style : myStyle[i],
		id : i
	});
	visible[i] = false;
	ol[i].on('add', function(ev) {
		visible[this.options.id] = true;
		if (this._map.getZoom() < (15 - this.options.id)) {
			return;
		}
		loadRoadsFromServer(this._map, this.options.id);
	});
	ol[i].on('remove', function(ev) {
		visible[this.options.id] = false;
	});
}

var weightslayer = L.layerGroup();

var availableOverLays = {
	"Häufigkeit 1" : ol[1],
	"Häufigkeit 2" : ol[2],
	"Häufigkeit 3" : ol[3],
	"Häufigkeit 4" : ol[4],
	"Häufigkeit 5" : ol[5],
	"Häufigkeit 6" : ol[6],
	"Häufigkeit 7" : ol[7],
	"Gewichte" : weightslayer
};

var weightsloaded = false;

module.exports.activeOverlayName = "Häufigkeit 7";
module.exports.defaultOverlay = ol[7];

module.exports.getAvailableOverlays = function() {
	return availableOverLays;
};

module.exports.loadOverlayContent = function(map) {
	if (!weightsloaded) {
		loadWeightsFromServer(weightslayer);
		weightsloaded = true;
	}
	return loadOftenUsedRoads(map);
};

function loadOftenUsedRoads(map) {
	for (i = 7; i >= 1; --i) {
		if (map.getZoom() < (15 - i)) {
			return;
		}
		if (visible[i]) {
			loadRoadsFromServer(map, i);
		}
	}
}

function loadRoadsFromServer(map, categorie) {
	// alert("moveend"); // ev is an event object (MouseEvent in this case)
	var request = new XMLHttpRequest();
	var bounds = map.getBounds();
	var requestData = "?zoom=" + map.getZoom() + "&minLon=" + bounds.getWest()
			+ "&maxLon=" + bounds.getEast() + "&minLat=" + bounds.getSouth()
			+ "&maxLat=" + bounds.getNorth() + "&categorie=" + categorie;

	var origin = "http://" + window.location.host + window.location.pathname;
	var url = origin + "popularEdges" + requestData;

	request.open("GET", url);
	request.addEventListener('load', function(event) {
		if (request.status >= 200 && request.status < 300) {
			var categorie = request.responseText.charAt(0);
			var jsondaten = JSON.parse(request.responseText.slice(1));
			ol[categorie].clearLayers();
			ol[categorie].addData(jsondaten);
		} else {
			console.warn(request.statusText, request.responseText);
			return null;
		}
	});
	request.send();
}

function loadWeightsFromServer(weightslayer) {
	var request = new XMLHttpRequest();

	//var origin = window.location.origin;
	var origin = "http://" + window.location.host + window.location.pathname;
	var url = origin + "weights";

	request.open("GET", url);
	request
			.addEventListener(
					'load',
					function(event) {
						if (request.status >= 200 && request.status < 300) {
							var weights = JSON.parse(request.responseText);
							for ( var i in weights) {
								var wlayer = L
										.marker(
												[ weights[i].lat,
														weights[i].lon ])
										.bindPopup(
												'Maximale Popularität: '
														+ weights[i].max_score
														+ '<br>Anzahl Wege: '
														+ weights[i].num_ways
//														+ '<br>Summe Score: '
//														+ weights[i].sum_score
														+ '<br>Durchschnittpopularität: '
														+ (weights[i].sum_score / weights[i].num_ways)
														+ '<br>Standardabweichung: '
														+ weights[i].stddev
														+ '<br>Varianz: '
														+ weights[i].varianz
														+ '<br>Quantile: '
														+ weights[i].quantil);
								weightslayer.addLayer(wlayer);
							}

						} else {
							console.warn(request.statusText,
									request.responseText);
							return null;
						}
					});
	request.send();
}
