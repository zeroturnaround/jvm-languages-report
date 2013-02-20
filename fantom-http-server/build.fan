using build
class Build : build::BuildPod
{
  new make()
  {
    podName = "FantomHttpProject"
    summary = ""
    srcDirs = [`./`, `fan/`]
    depends = ["build 1.0", "sys 1.0", "util 1.0", "concurrent 1.0"]
  }
}
