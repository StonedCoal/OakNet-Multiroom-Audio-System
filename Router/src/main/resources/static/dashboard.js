function onInfoCommand(data){
  for ( let input of data.inputs) {
    let amplitude = input.level / 32767
    let loudness = 20 * Math.log10(amplitude);
    loudness = loudness + 80;
    $("#progress-" + input.name).css("width", loudness + "%").attr('aria-valuenow', loudness);
    $( "span[id*='badge-" + input.name + "']" ).css("visibility", "hidden");

    /*
    // WHAT IN THE BLOODY FUKIN HELL DID I WROTE HERE?
    // However it seems to be working...
    let nodes = document.getElementsByTagName('button');
    for (let i = 0, n = nodes.length; i < n; ++i) {
      let d = nodes[i];
      if (d.parentNode && d.id.indexOf('toggle') >= 0) {
        for (let output of input.activeOutputs) {
          if(d.id.indexOf(output.name) >=0 && d.id.indexOf(input.name) >=0){
            d.innerHTML="Deactivate";
            break
          }
          d.innerHTML="Activate"
        }
      }
    }
    // End of mess
    */
    for (let output of input.activeOutputs) {
      $("#badge-" + input.name + "-" + output.name).css("visibility", "visible");
    }
  };
}


let socket = new WebSocket("ws://" + location.host + "/ws");

socket.onmessage = function (event) {
  // payload
  let payload = null;

  try {
    // Parse a JSON
    payload = JSON.parse(event.data);
  } catch (e) {
    // You can read e for more info
    // Let's assume the error is that we already have parsed the payload
    // So just return that
    payload = event.data;
  }

  switch (payload.command){
    case "info":
      onInfoCommand(JSON.parse(payload.data))
      break;
  }
}

  function activate(id, input, output){
    let button = document.getElementById(id);

    if(button.innerHTML == "Activate") {
      let payload = {
        command:"activate",
        data: JSON.stringify({
          input: {
            name: input
          },
          output: {
            name: output
          }
        })
      }
      socket.send(JSON.stringify(payload))
      button.innerHTML = "Deactivate";
    } else {
      let payload = {
        command:"deactivate",
        data:JSON.stringify({
          input: {
            name: input
          },
          output: {
            name: output
          },
        })
      }
      socket.send(JSON.stringify(payload))
      button.innerHTML = "Activate";
    }
  }

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
      network:{
        port:$("#vbanPort").val()
      },
      audioBackend:{
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