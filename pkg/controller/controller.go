package controller

import (
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	k8sscheme "k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/cache"
	"sigs.k8s.io/controller-runtime/pkg/manager"

	"github.com/redhat-appstudio/jvm-build-service/pkg/apis/jvmbuildservice/v1alpha1"
	"github.com/redhat-appstudio/jvm-build-service/pkg/reconciler/artifactbuild"
	"github.com/redhat-appstudio/jvm-build-service/pkg/reconciler/dependencybuild"

	pipelinev1beta1 "github.com/tektoncd/pipeline/pkg/apis/pipeline/v1beta1"
)

var (
	controllerLog = ctrl.Log.WithName("controller")
)

func NewManager(cfg *rest.Config, options manager.Options) (manager.Manager, error) {
	options.Scheme = runtime.NewScheme()

	// pretty sure this is there by default but we will be explicit like build-service
	if err := k8sscheme.AddToScheme(options.Scheme); err != nil {
		return nil, err
	}

	if err := v1alpha1.AddToScheme(options.Scheme); err != nil {
		return nil, err
	}

	if err := pipelinev1beta1.AddToScheme(options.Scheme); err != nil {
		return nil, err
	}
	options.NewCache = cache.BuilderWithOptions(cache.Options{
		SelectorsByObject: cache.SelectorsByObject{
			&pipelinev1beta1.TaskRun{}:  {Label: labels.SelectorFromSet(map[string]string{artifactbuild.TaskRunLabel: ""})},
			&v1alpha1.DependencyBuild{}: {},
			&v1alpha1.ArtifactBuild{}:   {},
		}})
	mgr, err := manager.New(cfg, options)
	if err != nil {
		return nil, err
	}

	if err := artifactbuild.SetupNewReconcilerWithManager(mgr); err != nil {
		return nil, err
	}

	if err := dependencybuild.SetupNewReconcilerWithManager(mgr); err != nil {
		return nil, err
	}

	return mgr, nil
}
