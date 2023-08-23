# Custom Discs v2.4 for Paper 1.16.5

A Paper fork of henkelmax's Audio Player.
- Воспроизведение пользовательских музыкальных дисков с использованием API простого голосового чата. (Мод голосового чата требуется на клиенте и сервере.)
- Используйте ```/customdisc``` или ```/cd```, чтобы создать пользовательский диск.
- Музыкальные файлы должны быть помещены в папку ```plugins/CustomDiscs/musicdata/```.
- Музыкальные файлы должны быть в форматах ```.wav```, ```.flac``` или ```.mp3```.

Загрузка файлов:
- Для загрузки файла используйте команду ```/cd download <url> <имя_файла.расширение>```. Ссылка, используемая для загрузки файла, должна быть прямой (то есть файл должен начать загружаться автоматически при доступе по ссылке). Файлы должны иметь правильное указанное расширение. В консоли сервера будет вызвано исключение UnsupportedAudioFileException, если расширение файла неправильное (например, при попытке дать файлу формата wav расширение mp3). Ниже приведен пример использования команды и ссылка для получения прямых загрузок из Google Drive.
- Пример: ```/cd download https://example.com/mysong "mysong.mp3"```
- Прямые ссылки Google Drive: https://lonedev6.github.io/gddl/

Пермишаны (необходимы для выполнения команд. Воспроизведение дисков не требует наличия разрешения.):
- ```customdiscs.create``` для создания диска
- ```customdiscs.download``` для загрузки файла


Зависимости:

- Данный плагин зависит от последней версии ProtocolLib для версии 1.16.5

https://user-images.githubusercontent.com/64107368/178426026-c454ac66-5133-4f3a-9af9-7f674e022423.mp4

Конфиг:
```
# [Music Disc Config]

# The distance from which music discs can be heard in blocks.
music-disc-distance: 16

# The master volume of music discs from 0-1. (You can set values like 0.5 for 50% volume).
music-disc-volume: 1

#The maximum download size in megabytes.
max-download-size: 50
```


