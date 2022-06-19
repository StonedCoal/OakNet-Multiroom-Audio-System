let inputTemplateFunc = input => `
<div id="input-${input}" class="accordion-item">
    <div class="accordion-header" id="heading-${input}">
        <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse"
            data-bs-target="#collapse-${input}" aria-expanded="false"
            aria-controls="collapse-${input}">
            <h6>Input ID: ${input}</h6>
        </button>
    </div>
    <div id="collapse-${input}" class="accordion-collapse collapse"
        aria-labelledby="heading-${input}" data-bs-parent="#accordionInputs">
        <div class="accordion-body">
            <div class="progress mt-2">
                <div class="progress-bar bg-primary" id="progress-${input}"
                    role="progressbar" aria-valuenow="25" aria-valuemin="0"
                    aria-valuemax="100">
                </div>
            </div>
            <br>
            <div id="${input}-outputs" class="d-flex flex-wrap">

            </div>
        </div>
    </div>
</div>`

let outputTemplateFunc = (input, output) => `
<div id="output-${output}-${input}" class="card m-1" style="width: 12rem;">
    <div class="card-body text-center">
        <h5 class="card-title">Output:</h5>
        <h6 class="card-subtitle mb-2 text-muted">${output}</h6>
        <span id="badge-${input}-${output}" class="badge bg-primary" style="visibility: hidden">Active</span>
        <br><br>
        <button id="toggle-${input}-${output}"
            class="btn btn-primary"
            onclick="activate(this.id, '${input}', '${output}')">Activate</button>
    </div>
 </div>`

let connectedClientTemplate = (output) =>`
<li id="connectedClient-${output}" class="list-group-item">
    <div class="d-flex justify-content-between align-items-start">
        <div class="fw-bold ms-2 me-auto">
        ${output}
        </div>
        <div class="mx-auto" id="${output}-currentlyPlaying">Currently Playing: Nothing</div>
        <div id="infolabel-${output}">Buffer: N/A|N/A, Volume: N/A</div>
    </div>
    <div class="d-flex justify-content-between align-items-start">
        <label for="volume-${output}" class="form-label ms-2" id="${output}-volume-label">Change Volume: </label>
        <input type="range" class="form-range ms-2" oninput="volumeChanged(this, '${output}')" id="${output}-volume-slider">
    </div>
    
</li>
`
let radioStationTemplate = (stationName, stationUrl) =>`
<li class="list-group-item d-flex justify-content-between align-items-start">
    <div class="ms-2 me-auto">
        ${stationName}
    </div>
    <button class="btn btn-primary me-2" onclick="playStation('${stationUrl}');">
        play
     </button>
</li>
`


let inputs = [];
let outputs = [];


function onInfoCommand(data) {
    for (let input of data.inputs) {
        let amplitude = input.level / 32767
        let loudness = 20 * Math.log10(amplitude);
        loudness = 80 + loudness;
        $("#progress-" + input.name).css("width", loudness + "%").attr('aria-valuenow', loudness);
    }
}

function onActivationEvent(payload) {
    let buttons = document.getElementsByTagName('button');
    let spans = document.getElementsByTagName('span');
    for (let i = 0, n = buttons.length; i < n; ++i) {
        let button = buttons[i];
        if (button.id.indexOf('toggle') >= 0) {
            if (button.id.indexOf(payload.output.name) >= 0 && button.id.indexOf(payload.input.name) >= 0) {
                button.innerHTML = "Deactivate";
            } else if (button.id.indexOf(payload.output.name) >= 0) {
                button.innerHTML = "Activate";
            }
        }
    }
    for (let i = 0, n = spans.length; i < n; ++i) {
        let span = spans[i];
        if (span.id.indexOf('badge') >= 0) {
            if (span.id.indexOf(payload.output.name) >= 0 && span.id.indexOf(payload.input.name) >= 0) {
                $(span).css("visibility", "visible");
            } else if (span.id.indexOf(payload.output.name) >= 0) {
                $(span).css("visibility", "hidden");
            }
        }
    }
    let currentlyPlaying = payload.input.name === "NOTHINGTOSEEHERELOLXD_JUSTALONGSTRINGWITHMORETHAN32CHARACTERSTOPREVENTACCIDENTIALBLOCKING" ? "Nothing" : payload.input.name;
    document.getElementById(payload.output.name + "-currentlyPlaying").innerHTML = "Currently Playing: " + currentlyPlaying;

}

function onNotifyEvent() {
    location.reload();
}

function onAddInputEvent(payload) {
    inputs.push(payload.name);
    $("#accordionInputs").append(inputTemplateFunc(payload.name));
    outputs.forEach(function (item, index, array) {
        $('#' + payload.name + '-outputs').append(outputTemplateFunc(payload.name, item));
    });
}

function onRemoveInputEvent(payload) {
    inputs = inputs.filter(element => element.name !== payload.name)
    $("#input-"+payload.name).remove();
}

function onAddOutputEvent(payload) {
    outputs.push(payload.name);
    inputs.forEach(function (item, index, array) {
        $('#' + item + '-outputs').append(outputTemplateFunc(item, payload.name));
    });
    $("#client-list").append(connectedClientTemplate(payload.name));
}

function onRemoveOutputEvent(payload) {
    outputs = outputs.filter(element => element.name !== payload.name);
    $("[id^=output-" + payload.name + "]").remove();
    $("#connectedClient-" + payload.name).remove();
}

function onNewRadioStationEvent(payload) {
    $("#radio-list").append(radioStationTemplate(payload.name, payload.url));
}

function onUpdateClientEvent(payload) {
    $("#infolabel-"+payload.name).html("Buffer: " + payload.currentBufferSize + "|" + payload.bufferGoal + ", Volume: " + payload.volume);
}

let socket = new WebSocket("ws://" + location.host + "/ws");

function checkSocket() {
    while (socket.readyState == socket.CLOSING) ;
    if (socket.readyState == socket.CLOSED)
        socket = new WebSocket("ws://" + location.host + "/ws");
    while (socket.readyState == socket.CONNECTING) ;
}

socket.onmessage = function (event) {
    // payload
    let payload = null;
    try {
        payload = JSON.parse(event.data);
    } catch (e) {
        payload = event.data;
    }

    switch (payload.command) {
        case "info":
            onInfoCommand(JSON.parse(payload.data));
            break;
        case "activationEvent":
            onActivationEvent(JSON.parse(payload.data));
            break;
        case "notify":
            onNotifyEvent();
            break;
        case "newInputEvent":
            onAddInputEvent(JSON.parse(payload.data));
            break;
        case "newOutputEvent":
            onAddOutputEvent(JSON.parse(payload.data));
            break;
        case "removeInputEvent":
            onRemoveInputEvent(JSON.parse(payload.data));
            break;
        case "removeOutputEvent":
            onRemoveOutputEvent(JSON.parse(payload.data));
            break;
        case "newRadioStationEvent":
            onNewRadioStationEvent(JSON.parse(payload.data));
            break;
        case "updateClientEvent":
            onUpdateClientEvent(JSON.parse(payload.data));
            break;
    }
}

function playStation(url) {
    let payload = {
        command: "playStation",
        data: JSON.stringify({
            url: url
        })
    };
    checkSocket();
    socket.send(JSON.stringify(payload));
}

function activate(id, input, output) {
    let button = document.getElementById(id);

    if (button.innerHTML === "Activate") {
        let payload = {
            command: "activate",
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
            command: "deactivate",
            data: JSON.stringify({
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

function volumeChanged(element, output) {
    let value = element.value;
    console.log(value);
}