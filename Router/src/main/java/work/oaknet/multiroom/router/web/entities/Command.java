package work.oaknet.multiroom.router.web.entities;

public class Command {

    private String command;
    private String data;

    public String getData() {
        return data;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setData(String data) {
        this.data = data;
    }
}
