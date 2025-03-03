# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

* jmus does no longer show the last played songs, but the song list.

### Added

* Added 'back' command.

### Fixed

* Long title infos are abbreviated with '...'.
* Status bar now shows the correct version.
* Volume is no longer reset when playing the next song.
* Display error, when reaching the end of the song list.
* Fixed index out of bounds error while drawing
* Fixed missing artist, album and title infos, if ID3v2 tags and empty v1 tags are present.

### Changed

* Screen is cleared on exit.

## [0.1.0] - 2024-07-11

* Initial version.
