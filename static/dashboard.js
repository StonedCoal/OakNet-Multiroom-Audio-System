(function() {
    var status = $('.status'),
      poll = function() {
        $.ajax({
          url: 'info',
          dataType: 'json',
          type: 'get',
          success: function(data) { 
            for (let [key, value] of Object.entries(data.inputs.input_levels)){
              var amplitude = data.inputs.input_levels[key] / 32767
              var loudness = 20 * Math.log10(amplitude);
              loudness = loudness + 80;
              $("#progress-"+key).css("width", loudness+"%").attr('aria-valuenow', loudness);
              $("#badge-"+key).css("visibility", data.inputs.active_input == key?"visible":"hidden");
            }
            var list=""
            for (let [key, value] of Object.entries(data.clients)){
              list+='<li class="list-group-item">'+ key +''+'</li>'
            }
            $("#client-list").html(list);
          },
          error: function() { // error logging
            console.log('Error!');
          }
        });
      },
      pollInterval = setInterval(function() { // run function every 200 ms
        poll();
        }, 50);
      poll(); // also run function on init
  })();

function changeInput(inputId){
  $.ajax({
    url: 'switchinput/'+inputId,
    dataType: 'none',
    type: 'get',
    success: function(data) { 
      
    },
    error: function() { 
      
    }
  });
};