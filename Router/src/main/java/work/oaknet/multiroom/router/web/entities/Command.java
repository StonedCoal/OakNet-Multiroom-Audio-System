package work.oaknet.multiroom.router.web.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import manifold.ext.props.rt.api.var;
import work.oaknet.multiroom.router.audio.AudioSourceManager;
import work.oaknet.multiroom.router.net.ClientManager;
import work.oaknet.multiroom.router.web.RadioPlayer;
import work.oaknet.multiroom.router.web.entities.Audio.ActivationPayload;
import work.oaknet.multiroom.router.web.entities.Audio.Input;
import work.oaknet.multiroom.router.web.entities.Audio.Output;
import work.oaknet.multiroom.router.web.entities.radio.PlayStationPayload;
import work.oaknet.multiroom.router.web.entities.radio.RadioStation;

public class Command {

    @var String command;
    @var String data;

    public static class Factory{
        private static ObjectMapper mapper = new ObjectMapper();

        public static Command radioStationAddedCommand(RadioStation payload){
            var result = new Command();
            result.command = "newRadioStationEvent";
            result.data = mapper.writeValueAsString(payload);
            return result;
        }

        public static Command activationCommand(ActivationPayload payload){
            var result = new Command();
            result.command = "activationEvent";
            result.data = mapper.writeValueAsString(payload);
            return result;
        }

        public static Command newInputCommand(Input payload){
            var result = new Command();
            result.command = "newInputEvent";
            result.data = mapper.writeValueAsString(payload);
            return result;
        }

        public static Command newOutputCommand(Output payload){
            var result = new Command();
            result.command = "newOutputEvent";
            result.data = mapper.writeValueAsString(payload);
            return result;
        }

        public static Command outputUpdateCommand(Output payload){
            var result = new Command();
            result.command = "updateClientEvent";
            result.data = mapper.writeValueAsString(payload);
            return result;
        }
    }
    public class Parser{
        public static void parseCommand(Command command){
            var mapper = new ObjectMapper();
            switch(command.command){
                case "activate" ->{
                    ActivationPayload payload = mapper.readValue(command.data, ActivationPayload.class);
                    var source = AudioSourceManager.getInstance().getSourceByName(payload.input.name);
                    var output = ClientManager.getInstance().getClientByName(payload.output.name);
                    if(source == null || output == null)
                        return;
                    AudioSourceManager.getInstance().setActiveSourceForClient(source, output);
                }
                case "deactivate" ->{
                    ActivationPayload payload = mapper.readValue(command.data, ActivationPayload.class);
                    var output = ClientManager.getInstance().getClientByName(payload.output.name);
                    if(output == null)
                        return;
                    AudioSourceManager.getInstance().setActiveSourceForClient(null, output);
                }
                case "playStation" ->{
                    PlayStationPayload payload = mapper.readValue(command.data, PlayStationPayload.class);
                    RadioPlayer.instance.play(payload.url);
                }
            }
        }
    }
}
