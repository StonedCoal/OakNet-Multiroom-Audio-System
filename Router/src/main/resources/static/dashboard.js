function onInfoCommand(data){
  for ( let input of data.inputs) {
    let amplitude = input.level / 32767
    let loudness = 20 * Math.log10(amplitude);
    loudness = loudness + 80;
    $("#progress-" + input.name).css("width", loudness + "%").attr('aria-valuenow', loudness);
    $( "span[id*='badge-" + input.name + "']" ).css("visibility", "hidden");
    for (let output of input.activeOutputs) {
      $("#badge-" + input.name + "-" + output.name).css("visibility", "visible");
    }
  };
}

function onActivationEvent(payload){
  let buttons = document.getElementsByTagName('button');
  for (let i = 0, n = buttons.length; i < n; ++i) {
    let button = buttons[i];
    if (button.id.indexOf('toggle') >= 0) {
      if(button.id.indexOf(payload.output.name) >= 0 && button.id.indexOf(payload.input.name) >= 0){
        button.innerHTML="Deactivate";
      }else if(button.id.indexOf(payload.output.name) >= 0) {
        button.innerHTML = "Activate";
      }
    }
  }
  let currentlyPlaying = payload.input.name==="NOTHINGTOSEEHERELOLXD_JUSTALONGSTRINGWITHMORETHAN32CHARACTERSTOPREVENTACCIDENTIALBLOCKING"?"Nothing":payload.input.name;
  document.getElementById(payload.output.name + "-currentlyPlaying").innerHTML="Currently Playing: " + currentlyPlaying;

}

function onNotifyEvent(){
  location.reload();
}

let socket = new WebSocket("ws://" + location.host + "/ws");

function checkSocket(){
  while(socket.readyState == socket.CLOSING);
  if(socket.readyState == socket.CLOSED)
    socket = new WebSocket("ws://" + location.host + "/ws");
  while(socket.readyState == socket.CONNECTING);
}

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
      onInfoCommand(JSON.parse(payload.data));
      break;
    case "activationEvent":
      onActivationEvent(JSON.parse(payload.data));
      break;
    case "notify":
      onNotifyEvent();
      break;
  }
}

function playStation(url){
  let payload = {
    command:"playStation",
    data: JSON.stringify({
      url: url
    })
  };
  checkSocket();
  socket.send(JSON.stringify(payload));
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
      };
      checkSocket();
      socket.send(JSON.stringify(payload));
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
      };
      checkSocket();
      socket.send(JSON.stringify(payload));
    }
  }
