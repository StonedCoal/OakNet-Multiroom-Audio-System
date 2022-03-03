import vlc, time

player = vlc.MediaPlayer("C:/Users/Fabian/Downloads/resin_res_music.mp3")
mods = player.audio_output_device_enum()

devices = list()

if mods:
    mod = mods
    while mod:
        mod = mod.contents
        devices.append(mod.device)
        mod = mod.next
print(devices)
player.audio_output_device_set(None, devices[4])

player.play()

time.sleep(10)