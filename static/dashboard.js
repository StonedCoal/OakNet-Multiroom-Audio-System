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
      type: 'post',
      success: function(data) { 
        
      },
      error: function() { 
        
      }
    });
  };

  function addStaticStream(inputId){
    $.ajax({
      url: 'addstaticstream/'+$("#static-address").val(),
      dataType: 'none',
      type: 'post',
      success: function(data) { 
        
      },
      error: function() { 
        
      }
    });
  };

  function saveSettings(){
    var checkedBoxes = document.querySelectorAll('input[name=deviceCheckbox]:checked');
    var result={
      VBAN:{
        port:$("#vbanPort").val(),
        outStreamName:$("#outputStreamName").val(),
        inStreamName:$("#inputStreamName").val(),
        selectedOutput:$("#inputStreamDevice").val()
      },
      activeInputDevices:{}
    };
    checkedBoxes.forEach(box => {
      result.activeInputDevices[box.value]=$("#input-device-description-"+box.value).val()
    });
    console.log(result)
    $.ajax({
      url: "savesettings",
      contentType: "application/json; charset=utf-8",
      type: 'post',
      data: JSON.stringify(result),
      success: function(data) { 
      },
      error: function() { 
        
      }
    });
    
    setTimeout(function () { location.reload(true); }, 3000);
  };