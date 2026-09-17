                        modifier = if (preset == ZenThemePreset.Aurora) Modifier.focusRequester(firstFocusRequester) else Modifier,
                        onFocus = onChildFocus,
                        onClick = { onStateChange(state.copy(preset = preset, receiver = null, glassOpacity = preset.glassOpacity, glowStrength = preset.glowStrength)) }
                    )
                }
            }
        }
        ThemeSection("Receiver") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ZenReceiverTheme.entries) { receiver ->
                    ReceiverCard(receiver, state.receiver == receiver, state.reducedMotion, onChildFocus) {
                        onStateChange(state.copy(receiver = receiver, glassOpacity = .60f, glowStrength = .68f))
                    }
                }
            }
        }
        ThemeSection("Background") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ZenBackgroundPreset.entries) { background ->
                    CompactOptionCard(background.label, state.background == background, state.preset.accent, onChildFocus) {
                        onStateChange(state.copy(background = background))
                    }
                }
            }
        }
        ThemeSection("Animation") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ZenAnimationSpeed.entries) { speed ->
                    CompactOptionCard(speed.label, !state.reducedMotion && state.animationSpeed == speed, state.preset.accent, onChildFocus) {
                        onStateChange(state.copy(animationSpeed = speed, reducedMotion = speed == ZenAnimationSpeed.Off))
                    }
                }
                item(key = "theme-option-reduced-motion") {
                    CompactOptionCard("Reduced Motion", state.reducedMotion, state.preset.accent, onChildFocus) {
                        onStateChange(state.copy(reducedMotion = !state.reducedMotion))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            ControlColumn("Blur", "${state.backgroundBlur}%", state.backgroundBlur / 100f, onChildFocus) {
                onStateChange(state.copy(backgroundBlur = (it * 100).toInt().coerceIn(0, 100)))
            }