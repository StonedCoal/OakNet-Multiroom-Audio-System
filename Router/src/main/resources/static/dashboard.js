(function() {
    var status = $('.status'),
      poll = function() {
        $.ajax({
          url: 'info',
          dataType: 'json',
          type: 'get',
          success: function(data) {
            //console.log(data);
            for ( let input of data.inputs) {
              let amplitude = input.level / 32767
              let loudness = 20 * Math.log10(amplitude);
              loudness = loudness + 80;
              $("#progress-" + input.name).css("width", loudness + "%").attr('aria-valuenow', loudness);
              $( "span[id*='badge-" + input.name + "']" ).css("visibility", "hidden");

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
              for (let output of input.activeOutputs) {
                $("#badge-" + input.name + "-" + output.name).css("visibility", "visible");
              }
            }
            /*
            let list=""
            for (let [key, value] of Object.entries(data.clients)){
              list+='<li class="list-group-item">'+ key +''+'</li>'
            }
            $("#client-list").html(list);
             */
          },
          error: function() { // error logging
            console.log('Error!');
          }
        });
      },
      pollInterval = setInterval(function() { // run function every 200 ms
        poll();
        }, 100);
      poll(); // also run function on init
  })();

  function activate(id, input, output){
    let button = document.getElementById(id);

    if(button.innerHTML == "Activate") {
      $.ajax({
        url: 'activate',
        dataType: 'none',
        type: 'post',
        data: input + ':' + output,
        success: function (data) {

        },
        error: function () {

        }
      });
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