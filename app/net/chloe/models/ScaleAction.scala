package net.chloe.models

trait ScaleAction

case object ScaleUp extends ScaleAction
case object ScaleDown extends ScaleAction
case object NoScaling extends ScaleAction