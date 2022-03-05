package work.oaknet.multiroom.router.web.entities.Audio;

import java.util.List;

public class Input {

    private String name;
    private int level;
    private List<Output> activeOutputs;

    public List<Output> getActiveOutputs() {
        return activeOutputs;
    }

    public void setActiveOutputs(List<Output> activeOutputs) {
        this.activeOutputs = activeOutputs;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
