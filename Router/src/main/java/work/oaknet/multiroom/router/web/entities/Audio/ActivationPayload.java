package work.oaknet.multiroom.router.web.entities.Audio;

public class ActivationPayload {

    private Input input;
    private Output output;

    public void setInput(Input input) {
        this.input = input;
    }

    public Input getInput() {
        return input;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public Output getOutput() {
        return output;
    }
}
