var currDate = new Date("Feb 18, 2011 00:00:00");

function loadTimeline(){	
    var dateMax = new Date("March 23, 2011 00:00:00");
    // var dateMin = new Date(dateMax.getTime() - 604800000*4);
    var dateMin = new Date("Feb 15, 2011 00:00:00");
	
    $(function() {

        $("#slider").slider({
            stop: function(event, ui) {
                currDate  = new Date(ui.value);
                reDrawMap();
            }
        },{
            range: "min",
            step: 86400000,
            min: dateMin.getTime(),
            max: dateMax.getTime(),
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