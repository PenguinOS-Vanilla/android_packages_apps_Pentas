# SPDX-FileCopyrightText: The Yasvara Project
# SPDX-License-Identifier: Apache-2.0
#
# Include this file in your device's BoardConfig.mk:
#   include packages/apps/Pentas/sepolicy/sepolicy.mk

SYSTEM_EXT_PUBLIC_SEPOLICY_DIRS += packages/apps/Pentas/sepolicy/public
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += packages/apps/Pentas/sepolicy/private
BOARD_VENDOR_SEPOLICY_DIRS += packages/apps/Pentas/sepolicy/vendor
