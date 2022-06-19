package work.oaknet.multiroom.router.web.entities.Audio;

import manifold.ext.props.rt.api.var;

public class Output {
   @var String name;
   @var int volume;
   @var int maxVolume;
   @var int currentBufferSize;
   @var int bufferGoal;
}
