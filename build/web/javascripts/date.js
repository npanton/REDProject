var currDate = new Date();

function loadData(){	
	var currentDate = new Date("December 31, 2011 00:00:00");
	var previousWeek = new Date(currentDate.getTime() - 604800000*52);
	$(function() {

		$("#slider").slider({
			stop: function(event, ui) {
				//getDateRange(false);
				currDate  = new Date(ui.value);
			}
		},{
			range: "min",
			step: 86400000,
			min: previousWeek.getTime(),
			max: currentDate.getTime(),
			value: currDate.getTime(),
			slide: function(event, ui) {
				currDate  = new Date(ui.value);
				$("#date").val(currDate.toDateString());
			}
		});
		var initDate  = new Date($("#slider").slider("value"));
		$("#date").val(initDate.toDateString());
	});
}