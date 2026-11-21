package com.example.tvplayer.data

import com.example.tvplayer.data.model.Channel

val defaultChannels = listOf(
    Channel("Test - Big Buck Bunny", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Test"),
    Channel("Test - Apple BipBop", "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8", "Test")
)

val epgPresets = listOf(
    Pair("France", "https://epgshare01.online/epgshare01/epg_ripper_FR1.xml.gz"),
    Pair("United Kingdom", "https://epgshare01.online/epgshare01/epg_ripper_UK1.xml.gz"),
    Pair("United States", "https://epgshare01.online/epgshare01/epg_ripper_US2.xml.gz"),
    Pair("Canada", "https://epgshare01.online/epgshare01/epg_ripper_CA2.xml.gz"),
    Pair("Germany", "https://epgshare01.online/epgshare01/epg_ripper_DE1.xml.gz"),
    Pair("Spain", "https://epgshare01.online/epgshare01/epg_ripper_ES1.xml.gz"),
    Pair("Italy", "https://epgshare01.online/epgshare01/epg_ripper_IT1.xml.gz"),
    Pair("Netherlands", "https://epgshare01.online/epgshare01/epg_ripper_NL1.xml.gz"),
    Pair("Portugal", "https://epgshare01.online/epgshare01/epg_ripper_PT1.xml.gz"),
    Pair("Ireland", "https://epgshare01.online/epgshare01/epg_ripper_IE1.xml.gz"),
    Pair("Australia", "https://epgshare01.online/epgshare01/epg_ripper_AU1.xml.gz"),
    Pair("New Zealand", "https://epgshare01.online/epgshare01/epg_ripper_NZ1.xml.gz"),
    Pair("India", "https://epgshare01.online/epgshare01/epg_ripper_IN1.xml.gz"),
    Pair("Arabic / UAE", "https://epgshare01.online/epgshare01/epg_ripper_AE1.xml.gz"),
    Pair("South Africa", "https://epgshare01.online/epgshare01/epg_ripper_ZA1.xml.gz"),
    Pair("Nigeria", "https://epgshare01.online/epgshare01/epg_ripper_NG1.xml.gz"),
    Pair("Plex (FAST)", "https://epgshare01.online/epgshare01/epg_ripper_PLEX1.xml.gz")
)
